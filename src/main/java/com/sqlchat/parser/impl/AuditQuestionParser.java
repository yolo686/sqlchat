package com.sqlchat.parser.impl;

import com.sqlchat.model.ParsedQuestion;
import com.sqlchat.parser.QuestionParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 审计问题解析器
 * @author sqlChat
 */
@Component
public class AuditQuestionParser implements QuestionParser {

    private static final String DOMAIN_BUDGET = "预算执行";
    private static final String DOMAIN_ENGINEERING = "工程投资";
    private static final String DOMAIN_NATURAL_RESOURCES = "自然资源";

    private static final Map<String, List<String>> DOMAIN_KEYWORDS = new HashMap<>();

    static {
        DOMAIN_KEYWORDS.put(DOMAIN_BUDGET, Arrays.asList(
                "预算", "执行率", "结转", "结余", "三公经费", "财政", "拨付", "专项资金", "决算"
        ));
        DOMAIN_KEYWORDS.put(DOMAIN_ENGINEERING, Arrays.asList(
                "工程", "项目", "概算", "决算", "招投标", "中标", "合同", "变更签证", "工期", "造价"
        ));
        DOMAIN_KEYWORDS.put(DOMAIN_NATURAL_RESOURCES, Arrays.asList(
                "土地", "耕地", "林地", "矿产", "资源", "出让金", "红线", "地块", "闲置土地", "开采"
        ));
    }

    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})年");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|万元|亿|亿元|元)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    @Override
    public ParsedQuestion parse(String question) {
        if (question == null || question.trim().isEmpty()) {
            return new ParsedQuestion("", "", null, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), "通用查询", Collections.emptyMap());
        }

        String normalizedQuestion = normalizeQuestion(question);
        String domain = detectDomain(normalizedQuestion);
        String intent = detectIntent(normalizedQuestion);
        Map<String, String> entities = extractEntities(normalizedQuestion);
        List<String> auditTerms = extractAuditTerms(normalizedQuestion);

        return new ParsedQuestion(
                question,
                normalizedQuestion,
                domain,
                auditTerms,
                Collections.emptyList(),
                Collections.emptyList(),
                intent,
                entities
        );
    }

    private String normalizeQuestion(String question) {
        return question.trim()
                .replaceAll("\\s+", " ")
                .replace("？", "?")
                .replace("，", ",")
                .replace("。", ".");
    }

    private String detectDomain(String question) {
        int bestScore = 0;
        String bestDomain = null;
        for (Map.Entry<String, List<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (question.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestDomain = entry.getKey();
            }
        }
        return bestDomain;
    }

    private String detectIntent(String question) {
        if (question.contains("排名") || question.contains("top") || question.contains("前")) {
            return "排名分析";
        }
        if (question.contains("趋势") || question.contains("变化") || question.contains("同比") || question.contains("环比")) {
            return "趋势分析";
        }
        if (question.contains("超") || question.contains("异常") || question.contains("预警") || question.contains("问题")) {
            return "异常筛查";
        }
        if (question.contains("占比") || question.contains("结构")) {
            return "结构分析";
        }
        if (question.contains("多少") || question.contains("总额") || question.contains("合计") || question.contains("汇总")) {
            return "统计汇总";
        }
        return "通用查询";
    }

    private Map<String, String> extractEntities(String question) {
        Map<String, String> entities = new LinkedHashMap<>();
        java.util.regex.Matcher yearMatcher = YEAR_PATTERN.matcher(question);
        if (yearMatcher.find()) {
            entities.put("year", yearMatcher.group(1));
        }
        java.util.regex.Matcher amountMatcher = AMOUNT_PATTERN.matcher(question);
        if (amountMatcher.find()) {
            entities.put("amount", amountMatcher.group(1) + amountMatcher.group(2));
        }
        java.util.regex.Matcher percentMatcher = PERCENT_PATTERN.matcher(question);
        if (percentMatcher.find()) {
            entities.put("percentage", percentMatcher.group(1) + "%");
        }
        return entities;
    }

    private List<String> extractAuditTerms(String question) {
        return DOMAIN_KEYWORDS.values().stream()
                .flatMap(Collection::stream)
                .filter(question::contains)
                .distinct()
                .collect(Collectors.toList());
    }

//    // 审计专业名词词典
//    private static final Set<String> AUDIT_TERMS = new HashSet<>(Arrays.asList(
//        "应收账款", "应付账款", "预收账款", "预付账款",
//        "营业收入", "营业成本", "净利润", "毛利润",
//        "资产总额", "负债总额", "所有者权益",
//        "现金流量", "经营活动现金流", "投资活动现金流", "筹资活动现金流",
//        "存货", "固定资产", "无形资产", "长期股权投资",
//        "短期借款", "长期借款", "应付票据", "应收票据",
//        "销售费用", "管理费用", "财务费用", "研发费用",
//        "审计", "审计报告", "审计意见", "审计证据",
//        "内部控制", "风险评估", "实质性程序", "控制测试",
//        "账龄分析", "坏账准备", "减值准备", "折旧", "摊销"
//    ));
//
//    // 常见表名关键词
//    private static final Set<String> TABLE_KEYWORDS = new HashSet<>(Arrays.asList(
//        "账", "表", "单", "记录", "明细", "汇总", "报表"
//    ));
//
//    // 常见列名关键词
//    private static final Set<String> COLUMN_KEYWORDS = new HashSet<>(Arrays.asList(
//        "金额", "数量", "日期", "时间", "编号", "代码", "名称", "类型", "状态"
//    ));
//
//    @Override
//    public ParsedQuestion parse(String question) {
//        if (question == null || question.trim().isEmpty()) {
//            return new ParsedQuestion();
//        }
//
//        String normalizedQuestion = normalizeQuestion(question);
//        List<String> auditTerms = extractAuditTerms(question);
//        List<String> tableNames = extractTableNames(question);
//        List<String> columnNames = extractColumnNames(question);
//        String intent = extractIntent(question);
//
//        ParsedQuestion parsedQuestion = new ParsedQuestion();
//        parsedQuestion.setOriginalQuestion(question);
//        parsedQuestion.setNormalizedQuestion(normalizedQuestion);
//        parsedQuestion.setAuditTerms(auditTerms);
//        parsedQuestion.setTableNames(tableNames);
//        parsedQuestion.setColumnNames(columnNames);
//        parsedQuestion.setIntent(intent);
//
//        return parsedQuestion;
//    }
//
//    /**
//     * 标准化问题
//     */
//    private String normalizeQuestion(String question) {
//        // 移除多余空格
//        String normalized = question.trim().replaceAll("\\s+", " ");
//        // 统一标点符号
//        normalized = normalized.replace("？", "?");
//        normalized = normalized.replace("，", ",");
//        normalized = normalized.replace("。", ".");
//        return normalized;
//    }
//
//    /**
//     * 提取审计专业名词
//     */
//    private List<String> extractAuditTerms(String question) {
//        return AUDIT_TERMS.stream()
//            .filter(term -> question.contains(term))
//            .collect(Collectors.toList());
//    }
//
//    /**
//     * 提取可能涉及的表名
//     */
//    private List<String> extractTableNames(String question) {
//        List<String> tableNames = new ArrayList<>();
//
//        // 查找包含表关键词的短语
//        for (String keyword : TABLE_KEYWORDS) {
//            Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]+" + keyword + ")");
//            java.util.regex.Matcher matcher = pattern.matcher(question);
//            while (matcher.find()) {
//                tableNames.add(matcher.group(1));
//            }
//        }
//
//        // 查找"XX表"、"XX单"等模式
//        Pattern tablePattern = Pattern.compile("([\\u4e00-\\u9fa5]+)(表|单|记录|明细)");
//        java.util.regex.Matcher matcher = tablePattern.matcher(question);
//        while (matcher.find()) {
//            tableNames.add(matcher.group(1) + matcher.group(2));
//        }
//
//        return tableNames.stream().distinct().collect(Collectors.toList());
//    }
//
//    /**
//     * 提取可能涉及的列名
//     */
//    private List<String> extractColumnNames(String question) {
//        List<String> columnNames = new ArrayList<>();
//
//        // 查找包含列关键词的短语
//        for (String keyword : COLUMN_KEYWORDS) {
//            Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]+" + keyword + ")");
//            java.util.regex.Matcher matcher = pattern.matcher(question);
//            while (matcher.find()) {
//                columnNames.add(matcher.group(1));
//            }
//        }
//
//        return columnNames.stream().distinct().collect(Collectors.toList());
//    }
//
//    /**
//     * 提取查询意图
//     */
//    private String extractIntent(String question) {
//        if (question.contains("多少") || question.contains("数量") || question.contains("金额")) {
//            return "统计查询";
//        } else if (question.contains("哪些") || question.contains("什么") || question.contains("列出")) {
//            return "列表查询";
//        } else if (question.contains("最大") || question.contains("最高") || question.contains("最多")) {
//            return "最大值查询";
//        } else if (question.contains("最小") || question.contains("最低") || question.contains("最少")) {
//            return "最小值查询";
//        } else if (question.contains("平均") || question.contains("均值")) {
//            return "平均值查询";
//        } else if (question.contains("合计") || question.contains("总计") || question.contains("汇总")) {
//            return "汇总查询";
//        } else if (question.contains("趋势") || question.contains("变化") || question.contains("增长")) {
//            return "趋势分析";
//        } else {
//            return "通用查询";
//        }
//    }
}
