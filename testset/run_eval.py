#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
NL2SQL 自动化评测 & 消融实验脚本
===============================================================

一、基本评测用法:
    python run_eval.py --dataset bird --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass 123456

    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass 123456

    python run_eval.py --dataset all --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass 123456

二、消融实验 (Ablation Study) 用法:

    # 全功能基线 (Full Model)
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation full

    # 消融实验: 关闭提问解析
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation no-parsing

    # 消融实验: 关闭SQL示例
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation no-sql-examples

    # 消融实验: 关闭RAG文档(通用文档+业务规则+术语映射)
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation no-rag-docs

    # 消融实验: 关闭所有RAG（SQL示例+RAG文档）
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation no-rag

    # 消融实验: 仅Schema + LLM（关闭所有增强）
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation bare

    # 消融实验: 开启投票
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation voting

    # 批量跑所有预设消融实验
    python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 \
        --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation all

三、本地数据库验证模式（不调API，仅验证gold SQL能否执行）:
    python run_eval.py --dataset bird --mode local-db \
        --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass 123456

四、预设消融方案列表 (--ablation 参数):
    full            全功能基线（默认）
    no-parsing      关闭提问解析
    no-sql-examples 关闭SQL示例检索
    no-rag-docs     关闭RAG文档（通用文档+业务规则+术语映射）
    no-rag          关闭所有RAG（SQL示例 + RAG文档）
    bare            裸模型（仅Schema + LLM，关闭所有增强模块）
    voting          全功能 + 候选投票
    all             批量运行以上所有方案

五、说明:
    - 脚本调用独立的 /api/nl2sql/eval 接口（无需登录Session）
    - 数据库名从测试用例的 "database" 字段读取，可用 --db-name 覆盖
    - --db-host/port/user/pass 直接传给eval接口，接口自行连接数据库获取Schema

六、评测指标:
    EX  - Execution Accuracy: 生成SQL执行结果与gold SQL执行结果一致
    EM  - Exact Match: SQL规范化后完全一致
    VR  - Valid Rate: 生成的SQL能成功执行
    TR  - Table Recall: 生成SQL引用的表覆盖gold_tables的比例
    CR  - Column Recall: 生成SQL引用的列覆盖gold_columns的比例
"""

import json
import re
import os
import sys
import time
import hashlib
import argparse
import requests
import pymysql
from collections import defaultdict, OrderedDict
from datetime import datetime
from copy import deepcopy

# Windows GBK 终端兼容：强制 stdout 使用 utf-8
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

# ====================== 消融预设配置 ======================

ABLATION_PRESETS = OrderedDict([
    ('full',            {'parsing': True,  'sql_examples': True,  'rag_docs': True,  'voting': False, 'label': '全功能基线'}),
    ('no-parsing',      {'parsing': False, 'sql_examples': True,  'rag_docs': True,  'voting': False, 'label': '关闭提问解析'}),
    ('no-sql-examples', {'parsing': True,  'sql_examples': False, 'rag_docs': True,  'voting': False, 'label': '关闭SQL示例'}),
    ('no-rag-docs',     {'parsing': True,  'sql_examples': True,  'rag_docs': False, 'voting': False, 'label': '关闭RAG文档'}),
    ('no-rag',          {'parsing': True,  'sql_examples': False, 'rag_docs': False, 'voting': False, 'label': '关闭所有RAG'}),
    ('bare',            {'parsing': False, 'sql_examples': False, 'rag_docs': False, 'voting': False, 'label': '裸模型(仅Schema+LLM)'}),
    ('voting',          {'parsing': True,  'sql_examples': True,  'rag_docs': True,  'voting': True,  'label': '全功能+投票'}),
])


# ====================== 工具函数 ======================

def normalize_sql(sql):
    """规范化SQL用于精确匹配"""
    if not sql:
        return ""
    s = sql.strip().rstrip(";").strip()
    s = re.sub(r'\s+', ' ', s)
    s = s.upper()
    s = re.sub(r'\s*,\s*', ', ', s)
    s = re.sub(r'\s*\(\s*', '(', s)
    s = re.sub(r'\s*\)\s*', ')', s)
    s = re.sub(r'\s*(=|<>|!=|>=|<=|>|<)\s*', r' \1 ', s)
    s = re.sub(r"'([^']*)'", lambda m: "'" + m.group(1).strip() + "'", s)
    return s.strip()


def extract_tables(sql):
    """从SQL中提取引用的表名"""
    if not sql:
        return set()
    tables = set()
    for m in re.finditer(r'(?:FROM|JOIN)\s+([a-zA-Z_]\w*)', sql, re.IGNORECASE):
        tables.add(m.group(1).lower())
    return tables


def extract_columns(sql):
    """从SQL中提取引用的列名（粗略）"""
    if not sql:
        return set()
    cols = set()
    s = re.sub(r"'[^']*'", '', sql)
    for m in re.finditer(r'(?:\w+\.)?([a-zA-Z_]\w*)', s, re.IGNORECASE):
        name = m.group(1).lower()
        keywords = {'select','from','where','join','on','and','or','not','in','as',
                     'group','by','order','having','limit','offset','between','like',
                     'is','null','case','when','then','else','end','asc','desc',
                     'distinct','count','sum','avg','min','max','round','concat',
                     'left','right','inner','outer','cross','natural','union','all',
                     'exists','any','some','insert','update','delete','create','table',
                     'ifnull','coalesce','datediff','curdate','year','month','day',
                     'now','current_timestamp','date','int','varchar','decimal','bigint',
                     'true','false','upper','lower','trim','length','substring',
                     'group_concat','cast','convert','over','partition','row_number',
                     'rank','dense_rank','lag','lead','with','recursive','quarter',
                     'abs','set','session','max_execution_time','character'}
        if name not in keywords:
            cols.add(name)
    return cols


def compute_result_hash(rows):
    """计算查询结果集的哈希（排序后）"""
    if not rows:
        return "EMPTY"
    try:
        sorted_rows = []
        for row in rows:
            sorted_items = sorted(row.items(), key=lambda x: x[0])
            sorted_rows.append(tuple((k, str(v) if v is not None else 'NULL') for k, v in sorted_items))
        sorted_rows.sort()
        content = str(sorted_rows)
        return hashlib.md5(content.encode('utf-8')).hexdigest()
    except Exception:
        return str(rows)


# ====================== 数据库执行 ======================

class DatabaseExecutor:
    def __init__(self, host, port, user, password, database):
        self.config = dict(host=host, port=int(port), user=user, password=password,
                           database=database, charset='utf8mb4',
                           cursorclass=pymysql.cursors.DictCursor, connect_timeout=10)

    def execute_query(self, sql, timeout=30):
        """执行SQL查询，返回 (success, rows_or_error)"""
        conn = None
        try:
            conn = pymysql.connect(**self.config)
            with conn.cursor() as cursor:
                cursor.execute(f"SET SESSION MAX_EXECUTION_TIME={timeout * 1000}")
                cursor.execute(sql)
                rows = cursor.fetchall()
                return True, rows
        except Exception as e:
            return False, str(e)
        finally:
            if conn:
                conn.close()

    def init_schema(self, schema_sql_path):
        """初始化数据库Schema（先禁用外键检查，避免DROP TABLE顺序问题）"""
        conn = None
        try:
            with open(schema_sql_path, 'r', encoding='utf-8') as f:
                sql_content = f.read()
            conn = pymysql.connect(**self.config)
            with conn.cursor() as cursor:
                cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
                for statement in sql_content.split(';'):
                    # 去掉注释行后再判断是否为空
                    lines = [line for line in statement.split('\n')
                             if line.strip() and not line.strip().startswith('--')]
                    stmt = '\n'.join(lines).strip()
                    if stmt:
                        cursor.execute(stmt)
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            conn.commit()
            return True, "Schema初始化成功"
        except Exception as e:
            return False, str(e)
        finally:
            if conn:
                conn.close()


# ====================== API调用 ======================

class NL2SqlEvalClient:
    """调用独立的 /api/nl2sql/eval 评测接口（无需登录）"""

    def __init__(self, base_url):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()

    def convert(self, question, db_host, db_port, db_user, db_pass, db_name,
                enable_voting=False, enable_parsing=True,
                enable_sql_examples=True, enable_rag_docs=True,
                user_id=None):
        """调用eval接口，直接传递数据库连接参数"""
        try:
            payload = {
                "question": question,
                "dbHost": db_host,
                "dbPort": db_port,
                "dbUser": db_user,
                "dbPass": db_pass,
                "dbName": db_name,
                "dbType": "mysql",
                "enableVoting": enable_voting,
                "enableQuestionParsing": enable_parsing,
                "enableSqlExamples": enable_sql_examples,
                "enableRagDocuments": enable_rag_docs,
                "executeSql": False,
            }
            if user_id:
                payload["userId"] = user_id

            r = self.session.post(f"{self.base_url}/api/nl2sql/eval",
                                  json=payload, timeout=180)
            if r.status_code == 200:
                data = r.json()
                return data.get('sql', ''), data
            else:
                return '', {"error": f"HTTP {r.status_code}: {r.text[:200]}"}
        except Exception as e:
            return '', {"error": str(e)}


# ====================== 评测引擎 ======================

class Evaluator:
    def __init__(self, db_executor=None, api_client=None,
                 db_host=None, db_port=None, db_user=None, db_pass=None,
                 db_name_override=None, user_id=None,
                 ablation_config=None):
        self.db_executor = db_executor
        self.api_client = api_client
        self.db_host = db_host
        self.db_port = db_port
        self.db_user = db_user
        self.db_pass = db_pass
        self.db_name_override = db_name_override  # 如果指定，覆盖测试用例的 database 字段
        self.user_id = user_id
        self.ablation_config = ablation_config or ABLATION_PRESETS['full']
        self.results = []

    def _get_db_name(self, tc):
        """获取测试用例对应的数据库名：优先 --db-name，否则从用例 database 字段读取"""
        if self.db_name_override:
            return self.db_name_override
        return tc.get('database', '')

    def evaluate_test_cases(self, test_cases, mode='api'):
        """评测一组测试用例"""
        total = len(test_cases)
        cfg = self.ablation_config
        for i, tc in enumerate(test_cases):
            question_preview = tc['question'][:40]
            print(f"\r  [{i+1}/{total}] {tc['id']}: {question_preview}...", end='', flush=True)

            result = {
                'id': tc['id'],
                'database': tc.get('database', ''),
                'difficulty': tc.get('difficulty', 'Unknown'),
                'intent': tc.get('intent', 'Unknown'),
                'question': tc['question'],
                'gold_sql': tc['gold_sql'],
                'generated_sql': '',
                'ex': False,
                'em': False,
                'vr': False,
                'tr': 0.0,
                'cr': 0.0,
                'error': None,
                'time_ms': 0
            }

            start_time = time.time()

            if mode == 'api':
                db_name = self._get_db_name(tc)
                gen_sql, api_response = self.api_client.convert(
                    tc['question'],
                    db_host=self.db_host,
                    db_port=self.db_port,
                    db_user=self.db_user,
                    db_pass=self.db_pass,
                    db_name=db_name,
                    enable_voting=cfg.get('voting', False),
                    enable_parsing=cfg.get('parsing', True),
                    enable_sql_examples=cfg.get('sql_examples', True),
                    enable_rag_docs=cfg.get('rag_docs', True),
                    user_id=self.user_id,
                )
                result['generated_sql'] = gen_sql
                if cfg.get('voting'):
                    result['voting_strategy'] = api_response.get('votingStrategy', '')
                    result['confidence'] = api_response.get('confidence', 0)
                    result['degraded'] = api_response.get('degraded', False)
                # 记录服务端回显的消融配置
                ablation_echo = api_response.get('ablationConfig')
                if ablation_echo:
                    result['ablation_echo'] = ablation_echo
                # 记录错误
                if api_response.get('error'):
                    result['error'] = api_response['error']
                elif api_response.get('errorMessage'):
                    result['error'] = api_response['errorMessage']
            elif mode == 'local-db':
                result['generated_sql'] = tc['gold_sql']

            result['time_ms'] = int((time.time() - start_time) * 1000)

            gen_sql = result['generated_sql']

            # EM: Exact Match
            result['em'] = normalize_sql(gen_sql) == normalize_sql(tc['gold_sql'])

            # TR: Table Recall
            gold_tables = set(t.lower() for t in tc.get('gold_tables', []))
            gen_tables = extract_tables(gen_sql)
            if gold_tables:
                result['tr'] = len(gold_tables & gen_tables) / len(gold_tables)

            # CR: Column Recall
            gold_cols = set(c.lower() for c in tc.get('gold_columns', []))
            gen_cols = extract_columns(gen_sql)
            if gold_cols:
                result['cr'] = len(gold_cols & gen_cols) / len(gold_cols)

            # VR + EX: 需要数据库执行
            if self.db_executor and gen_sql:
                gen_ok, gen_result = self.db_executor.execute_query(gen_sql)
                result['vr'] = gen_ok

                if gen_ok:
                    gold_ok, gold_result = self.db_executor.execute_query(tc['gold_sql'])
                    if gold_ok:
                        result['ex'] = compute_result_hash(gen_result) == compute_result_hash(gold_result)
                    else:
                        result['error'] = f"Gold SQL执行失败: {gold_result}"
                else:
                    result['error'] = f"生成SQL执行失败: {gen_result}"

            self.results.append(result)
            time.sleep(0.1)

        print()

    def compute_metrics(self):
        """计算汇总指标"""
        if not self.results:
            return {}

        total = len(self.results)
        metrics = {
            'total': total,
            'EX': sum(1 for r in self.results if r['ex']) / total * 100,
            'EM': sum(1 for r in self.results if r['em']) / total * 100,
            'VR': sum(1 for r in self.results if r['vr']) / total * 100,
            'TR': sum(r['tr'] for r in self.results) / total * 100,
            'CR': sum(r['cr'] for r in self.results) / total * 100,
            'avg_time_ms': sum(r['time_ms'] for r in self.results) / total,
        }

        # 按难度分层
        by_difficulty = defaultdict(list)
        for r in self.results:
            by_difficulty[r['difficulty']].append(r)

        metrics['by_difficulty'] = {}
        for diff, items in sorted(by_difficulty.items()):
            n = len(items)
            metrics['by_difficulty'][diff] = {
                'count': n,
                'EX': sum(1 for r in items if r['ex']) / n * 100,
                'EM': sum(1 for r in items if r['em']) / n * 100,
                'VR': sum(1 for r in items if r['vr']) / n * 100,
            }

        # 按意图分层
        by_intent = defaultdict(list)
        for r in self.results:
            by_intent[r['intent']].append(r)

        metrics['by_intent'] = {}
        for intent, items in sorted(by_intent.items()):
            n = len(items)
            metrics['by_intent'][intent] = {
                'count': n,
                'EX': sum(1 for r in items if r['ex']) / n * 100,
            }

        return metrics

    def print_report(self, dataset_name, ablation_label, metrics):
        """打印评测报告"""
        print(f"\n{'='*70}")
        print(f"  评测报告: {dataset_name}")
        print(f"  消融配置: {ablation_label}")
        print(f"{'='*70}")
        print(f"  测试用例数: {metrics['total']}")
        print(f"  平均耗时:   {metrics['avg_time_ms']:.0f} ms")
        print(f"{'─'*70}")
        print(f"  {'指标':<25} {'值':>10}")
        print(f"{'─'*70}")
        print(f"  {'Execution Accuracy (EX)':<25} {metrics['EX']:>9.1f}%")
        print(f"  {'Exact Match (EM)':<25} {metrics['EM']:>9.1f}%")
        print(f"  {'Valid SQL Rate (VR)':<25} {metrics['VR']:>9.1f}%")
        print(f"  {'Table Recall (TR)':<25} {metrics['TR']:>9.1f}%")
        print(f"  {'Column Recall (CR)':<25} {metrics['CR']:>9.1f}%")

        if metrics.get('by_difficulty'):
            print(f"\n{'─'*70}")
            print(f"  按难度分层:")
            print(f"  {'难度':<15} {'数量':>6} {'EX':>8} {'EM':>8} {'VR':>8}")
            for diff, dm in metrics['by_difficulty'].items():
                print(f"  {diff:<15} {dm['count']:>6} {dm['EX']:>7.1f}% {dm['EM']:>7.1f}% {dm['VR']:>7.1f}%")

        if metrics.get('by_intent'):
            print(f"\n{'─'*70}")
            print(f"  按意图分层:")
            print(f"  {'意图':<15} {'数量':>6} {'EX':>8}")
            for intent, im in metrics['by_intent'].items():
                print(f"  {intent:<15} {im['count']:>6} {im['EX']:>7.1f}%")

        print(f"{'='*70}\n")

    def save_results(self, output_path, dataset_name, ablation_name, ablation_config, metrics):
        """保存详细结果到JSON"""
        output = {
            'dataset': dataset_name,
            'ablation': ablation_name,
            'ablation_config': ablation_config,
            'timestamp': datetime.now().isoformat(),
            'metrics': metrics,
            'details': self.results
        }
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(output, f, ensure_ascii=False, indent=2)
        print(f"  详细结果已保存至: {output_path}")


def print_ablation_comparison(all_results):
    """打印消融实验对比表"""
    if len(all_results) < 2:
        return

    print(f"\n{'#'*70}")
    print(f"#  消融实验对比汇总 (Ablation Study Comparison)")
    print(f"{'#'*70}\n")

    header = f"  {'方案':<25} {'EX':>8} {'EM':>8} {'VR':>8} {'TR':>8} {'CR':>8} {'Avg ms':>8}"
    print(header)
    print(f"  {'─'*73}")

    baseline_ex = None
    for name, label, metrics in all_results:
        ex = metrics['EX']
        if baseline_ex is None:
            baseline_ex = ex
            delta_str = "  (base)"
        else:
            delta = ex - baseline_ex
            delta_str = f"  ({delta:+.1f})" if delta != 0 else "  (=)"

        print(f"  {label:<25} {metrics['EX']:>7.1f}% {metrics['EM']:>7.1f}% "
              f"{metrics['VR']:>7.1f}% {metrics['TR']:>7.1f}% {metrics['CR']:>7.1f}% "
              f"{metrics['avg_time_ms']:>7.0f}{delta_str}")

    print()


# ====================== 主入口 ======================

DATASETS = ['spider', 'bird', 'audit_natural_resource', 'audit_budget_execution', 'audit_engineering_investment']

def resolve_ablation_configs(args):
    """根据命令行参数解析要运行的消融配置列表，返回 [(name, config), ...]"""
    ablation_name = args.ablation

    if ablation_name == 'all':
        return list(ABLATION_PRESETS.items())

    if ablation_name and ablation_name in ABLATION_PRESETS:
        return [(ablation_name, ABLATION_PRESETS[ablation_name])]

    # 自定义模式：使用 --enable-xxx / --disable-xxx 参数
    config = {
        'parsing': True,
        'sql_examples': True,
        'rag_docs': True,
        'voting': False,
        'label': '自定义配置'
    }

    if args.disable_parsing:
        config['parsing'] = False
    if hasattr(args, 'enable_parsing') and args.enable_parsing:
        config['parsing'] = True

    if args.disable_sql_examples:
        config['sql_examples'] = False
    if hasattr(args, 'enable_sql_examples') and args.enable_sql_examples:
        config['sql_examples'] = True

    if args.disable_rag_docs:
        config['rag_docs'] = False
    if hasattr(args, 'enable_rag_docs') and args.enable_rag_docs:
        config['rag_docs'] = True

    if args.enable_voting:
        config['voting'] = True
    if args.disable_voting:
        config['voting'] = False

    # 生成label
    parts = []
    parts.append(f"解析={'ON' if config['parsing'] else 'OFF'}")
    parts.append(f"SQL示例={'ON' if config['sql_examples'] else 'OFF'}")
    parts.append(f"RAG文档={'ON' if config['rag_docs'] else 'OFF'}")
    parts.append(f"投票={'ON' if config['voting'] else 'OFF'}")
    config['label'] = '自定义: ' + ', '.join(parts)

    return [('custom', config)]


def collect_database_names(test_cases):
    """从测试用例中收集所有不同的 database 名称"""
    names = set()
    for tc in test_cases:
        db = tc.get('database', '')
        if db:
            names.add(db)
    return names


def main():
    parser = argparse.ArgumentParser(
        description='NL2SQL 自动化评测 & 消融实验脚本',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
消融实验预设方案 (--ablation):
  full              全功能基线（默认）
  no-parsing        关闭提问解析（领域识别/意图分类/实体抽取）
  no-sql-examples   关闭SQL示例检索
  no-rag-docs       关闭RAG文档（通用文档+业务规则+术语映射）
  no-rag            关闭所有RAG（SQL示例 + RAG文档）
  bare              裸模型（仅Schema + LLM，关闭所有增强模块）
  voting            全功能 + 候选投票
  all               批量运行以上全部方案并输出对比表

说明:
  脚本调用独立的 /api/nl2sql/eval 评测接口（无需登录Session）。
  数据库名自动从测试用例的 "database" 字段读取，用 --db-name 可覆盖。
  eval接口根据传入的 --db-host/port/user/pass 直接连接数据库获取Schema。

示例:
  # 基本评测 (bird数据集)
  python run_eval.py --dataset bird --api http://localhost:8080 --db-host 127.0.0.1 --db-user root --db-pass 123456

  # 消融实验
  python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation no-sql-examples

  # 批量消融对比
  python run_eval.py --dataset audit_natural_resource --api http://localhost:8080 --db-host 127.0.0.1 --db-user root --db-pass 123456 --ablation all

  # 指定数据库名（覆盖测试用例中的database字段）
  python run_eval.py --dataset bird --api http://localhost:8080 --db-host 127.0.0.1 --db-user root --db-pass 123456 --db-name test_bird
        """)

    parser.add_argument('--dataset', required=True, help='数据集名称 或 all')
    parser.add_argument('--mode', default='api', choices=['api', 'local-db'], help='评测模式: api调用eval接口 / local-db本地验证gold SQL')

    # API模式参数
    parser.add_argument('--api', default='http://localhost:8080', help='NL2SQL API地址')

    # 数据库连接参数（同时用于eval接口传参 和 本地EX/VR验证）
    db_group = parser.add_argument_group('数据库连接参数')
    db_group.add_argument('--db-host', default='127.0.0.1', help='数据库主机')
    db_group.add_argument('--db-port', type=int, default=3306, help='数据库端口')
    db_group.add_argument('--db-user', default='root', help='数据库用户')
    db_group.add_argument('--db-pass', default='', help='数据库密码')
    db_group.add_argument('--db-name', default='', help='覆盖数据库名（不指定则从测试用例database字段读取）')
    db_group.add_argument('--init-schema', action='store_true', help='初始化测试数据库Schema')

    # 消融实验参数
    ablation_group = parser.add_argument_group('消融实验参数')
    ablation_group.add_argument('--ablation', default='full',
                                help='消融预设: full/no-parsing/no-sql-examples/no-rag-docs/no-rag/bare/voting/all')
    ablation_group.add_argument('--enable-parsing', action='store_true', default=False, help='开启提问解析')
    ablation_group.add_argument('--disable-parsing', action='store_true', default=False, help='关闭提问解析')
    ablation_group.add_argument('--enable-sql-examples', action='store_true', default=False, help='开启SQL示例')
    ablation_group.add_argument('--disable-sql-examples', action='store_true', default=False, help='关闭SQL示例')
    ablation_group.add_argument('--enable-rag-docs', action='store_true', default=False, help='开启RAG文档')
    ablation_group.add_argument('--disable-rag-docs', action='store_true', default=False, help='关闭RAG文档')
    ablation_group.add_argument('--enable-voting', action='store_true', default=False, help='开启候选投票')
    ablation_group.add_argument('--disable-voting', action='store_true', default=False, help='关闭候选投票')

    # 可选
    parser.add_argument('--user-id', default='', help='用户ID（用于RAG知识库检索，留空则不使用知识库）')
    parser.add_argument('--output-dir', default='', help='结果输出目录')
    parser.add_argument('--limit', type=int, default=0, help='限制测试用例数量（0=全部），用于快速验证')

    args = parser.parse_args()

    # local-db 模式自动开启 init-schema
    if args.mode == 'local-db':
        args.init_schema = True

    datasets_to_run = DATASETS if args.dataset == 'all' else [args.dataset]
    base_dir = os.path.dirname(os.path.abspath(__file__))

    # 解析消融配置
    ablation_configs = resolve_ablation_configs(args)
    print(f"\n消融实验方案数: {len(ablation_configs)}")
    for name, cfg in ablation_configs:
        print(f"  - {name}: {cfg['label']}  "
              f"[解析={cfg['parsing']}, SQL示例={cfg['sql_examples']}, "
              f"RAG文档={cfg['rag_docs']}, 投票={cfg['voting']}]")

    # 初始化API客户端（eval接口无需登录）
    api_client = None
    if args.mode == 'api':
        api_client = NL2SqlEvalClient(args.api)
        print(f"\n评测API地址: {args.api}/api/nl2sql/eval (独立接口，无需登录)")
        print(f"数据库连接: {args.db_user}@{args.db_host}:{args.db_port}")
        if args.db_name:
            print(f"数据库名覆盖: {args.db_name}")
        else:
            print(f"数据库名: 从测试用例 database 字段读取")

    for ds_name in datasets_to_run:
        ds_dir = os.path.join(base_dir, ds_name)
        tc_path = os.path.join(ds_dir, 'test_cases.json')
        schema_path = os.path.join(ds_dir, 'schema.sql')

        if not os.path.exists(tc_path):
            print(f"跳过 {ds_name}: 找不到 {tc_path}")
            continue

        print(f"\n{'#'*70}")
        print(f"# 数据集: {ds_name}")
        print(f"{'#'*70}")

        # 加载测试用例
        with open(tc_path, 'r', encoding='utf-8') as f:
            test_data = json.load(f)
        test_cases = test_data.get('test_cases', test_data) if isinstance(test_data, dict) else test_data

        if args.limit > 0:
            test_cases = test_cases[:args.limit]
            print(f"  [!] 限制模式: 仅评测前 {args.limit} 条")

        print(f"  加载 {len(test_cases)} 条测试用例")

        # 收集测试用例中的数据库名
        db_names_in_cases = collect_database_names(test_cases)
        if db_names_in_cases:
            print(f"  测试用例涉及数据库: {', '.join(sorted(db_names_in_cases))}")

        # 确定本地验证用的数据库名（用于 init-schema 和 EX/VR 验证）
        # 如果指定了 --db-name，使用它；否则用 test_{dataset_name}
        local_db_name = args.db_name or f"test_{ds_name}"

        # 初始化数据库
        db_executor = None
        should_connect_db = (args.mode == 'local-db') or args.db_pass
        if should_connect_db and args.db_host and args.db_user:
            try:
                if args.init_schema and os.path.exists(schema_path):
                    print(f"  初始化数据库 {local_db_name} ...")
                    init_conn = pymysql.connect(host=args.db_host, port=args.db_port,
                                                user=args.db_user, password=args.db_pass, charset='utf8mb4')
                    with init_conn.cursor() as cur:
                        cur.execute(f"CREATE DATABASE IF NOT EXISTS `{local_db_name}` CHARACTER SET utf8mb4")
                    init_conn.close()

                    tmp_exec = DatabaseExecutor(args.db_host, args.db_port, args.db_user, args.db_pass, local_db_name)
                    ok, msg = tmp_exec.init_schema(schema_path)
                    print(f"  Schema初始化: {'成功' if ok else '失败 - ' + msg}")

                db_executor = DatabaseExecutor(args.db_host, args.db_port, args.db_user, args.db_pass, local_db_name)
            except Exception as e:
                print(f"  数据库连接失败: {e}")

        # 收集所有消融结果用于对比
        ablation_results = []

        for abl_name, abl_config in ablation_configs:
            print(f"\n  ━━━ 消融方案: {abl_config['label']} ({abl_name}) ━━━")
            flags_str = (f"    解析={abl_config['parsing']}, SQL示例={abl_config['sql_examples']}, "
                         f"RAG文档={abl_config['rag_docs']}, 投票={abl_config['voting']}")
            print(flags_str)

            evaluator = Evaluator(
                db_executor=db_executor,
                api_client=api_client,
                db_host=args.db_host,
                db_port=args.db_port,
                db_user=args.db_user,
                db_pass=args.db_pass,
                db_name_override=args.db_name if args.db_name else None,
                user_id=args.user_id if args.user_id else None,
                ablation_config=abl_config,
            )

            print(f"  开始评测 (mode={args.mode}) ...")
            evaluator.evaluate_test_cases(test_cases, mode=args.mode)

            metrics = evaluator.compute_metrics()
            evaluator.print_report(ds_name, abl_config['label'], metrics)

            # 保存结果
            out_dir = args.output_dir or os.path.join(base_dir, 'results')
            os.makedirs(out_dir, exist_ok=True)
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            out_file = os.path.join(out_dir, f"{ds_name}_{abl_name}_{timestamp}.json")
            evaluator.save_results(out_file, ds_name, abl_name, abl_config, metrics)

            ablation_results.append((abl_name, abl_config['label'], metrics))

        # 如果有多组消融实验，输出对比表
        if len(ablation_results) > 1:
            print_ablation_comparison(ablation_results)

            # 保存对比汇总
            out_dir = args.output_dir or os.path.join(base_dir, 'results')
            comparison_file = os.path.join(out_dir, f"{ds_name}_ablation_comparison_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
            comparison_data = {
                'dataset': ds_name,
                'timestamp': datetime.now().isoformat(),
                'comparison': [
                    {'ablation': name, 'label': label, 'metrics': {k: v for k, v in m.items() if k not in ('by_difficulty', 'by_intent')}}
                    for name, label, m in ablation_results
                ]
            }
            with open(comparison_file, 'w', encoding='utf-8') as f:
                json.dump(comparison_data, f, ensure_ascii=False, indent=2)
            print(f"  消融对比汇总已保存至: {comparison_file}")


if __name__ == '__main__':
    main()
