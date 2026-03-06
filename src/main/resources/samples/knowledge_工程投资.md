domain: 工程投资
type: SQL_EXAMPLE
question: 查询超概算的工程项目

SELECT ps.project_code, ps.project_name,
       ps.approved_investment, ps.actual_investment,
       ps.overbudget_amount,
       ps.overbudget_rate
FROM project_settlement ps
WHERE ps.overbudget_amount > 0
ORDER BY ps.overbudget_rate DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询未经招投标的工程合同

SELECT pc.project_code, pi.project_name,
       pc.contract_code, pc.contract_name,
       pc.contract_type, pc.contractor, pc.contract_amount,
       pc.is_bidding, pc.bidding_method
FROM project_contract pc
JOIN project_info pi ON pc.project_code = pi.project_code
WHERE pc.is_bidding = '否'
ORDER BY pc.contract_amount DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 统计各项目工程变更金额及占合同比例

SELECT pi.project_name, pc.contract_code, pc.contract_name,
       pc.contract_amount,
       SUM(pch.change_amount) AS total_change_amount,
       ROUND(SUM(pch.change_amount) / pc.contract_amount * 100, 2) AS change_rate
FROM project_change pch
JOIN project_contract pc ON pch.project_code = pc.project_code AND pch.contract_code = pc.contract_code
JOIN project_info pi ON pch.project_code = pi.project_code
WHERE pch.approve_status = '已批准'
GROUP BY pi.project_name, pc.contract_code, pc.contract_name, pc.contract_amount
HAVING change_rate > 10
ORDER BY change_rate DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询工期严重延误的项目

SELECT project_code, project_name, project_type, build_unit,
       total_investment, plan_end_date, actual_end_date,
       DATEDIFF(IFNULL(actual_end_date, CURDATE()), plan_end_date) AS delay_days,
       project_status
FROM project_info
WHERE (actual_end_date > plan_end_date OR (actual_end_date IS NULL AND plan_end_date < CURDATE()))
ORDER BY delay_days DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询超付工程款的合同

SELECT pc.project_code, pi.project_name,
       pc.contract_code, pc.contract_name, pc.contract_amount,
       SUM(pp.paid_amount) AS total_paid,
       (SUM(pp.paid_amount) - pc.contract_amount) AS over_paid
FROM project_payment pp
JOIN project_contract pc ON pp.project_code = pc.project_code AND pp.contract_code = pc.contract_code
JOIN project_info pi ON pp.project_code = pi.project_code
GROUP BY pc.project_code, pi.project_name, pc.contract_code, pc.contract_name, pc.contract_amount
HAVING total_paid > pc.contract_amount
ORDER BY over_paid DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询各类型工程的投资完成情况

SELECT project_type,
       COUNT(*) AS project_count,
       SUM(total_investment) AS plan_total,
       SUM(govt_investment) AS govt_total,
       SUM(ps.actual_investment) AS actual_total,
       ROUND(SUM(ps.actual_investment) / SUM(total_investment) * 100, 2) AS completion_rate
FROM project_info pi
LEFT JOIN project_settlement ps ON pi.project_code = ps.project_code
GROUP BY project_type
ORDER BY plan_total DESC;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询同一承包商中标金额汇总排名

SELECT contractor,
       COUNT(*) AS contract_count,
       SUM(contract_amount) AS total_amount,
       GROUP_CONCAT(DISTINCT contract_type) AS contract_types
FROM project_contract
GROUP BY contractor
ORDER BY total_amount DESC
LIMIT 20;

---

domain: 工程投资
type: SQL_EXAMPLE
question: 查询竣工决算审减率排名

SELECT project_code, project_name,
       actual_investment,
       audit_reduce_amount,
       ROUND(audit_reduce_amount / actual_investment * 100, 2) AS reduce_rate,
       audit_status
FROM project_settlement
WHERE audit_reduce_amount > 0
ORDER BY reduce_rate DESC;

---

domain: 工程投资
type: GENERAL_DOC

工程投资审计主要内容：
1. 项目立项审计：审查项目建设程序是否合规，是否取得必要审批手续，可行性研究是否充分。
2. 招投标审计：审查招标方式是否合规，是否存在围标串标、肢解发包、化整为零规避招标等问题。
3. 合同管理审计：审查合同签订是否规范，合同价款是否合理，变更签证是否经过审批。
4. 工程造价审计：审查工程量计算是否准确，材料价格是否合理，取费标准是否正确。
5. 资金管理审计：审查资金拨付是否按进度和合同约定执行，是否存在超付、挪用、截留。
6. 竣工决算审计：审查决算是否真实完整，是否存在虚增造价、重复计量等问题。

---

domain: 工程投资
type: GENERAL_DOC

工程招投标审计关注点：
1. 必须招标范围：政府投资项目、国有资金占控股或主导地位的项目。
2. 必须公开招标的标准：施工单项合同估算价200万元以上，设备采购100万元以上，勘察设计50万元以上。
3. 常见违规行为：
   - 应招未招：达到招标标准但未进行招标
   - 拆分合同：将一个项目拆分为多个合同以规避招标
   - 围标串标：多家投标单位串通抬高或压低投标报价
   - 虚假招标：内定中标人后走招标程序
   - 先施工后招标：工程已开工但补办招标手续

---

domain: 工程投资
type: BUSINESS_RULE

工程变更签证审计规则：
1. 变更金额比例：单项变更 change_amount / contract_amount > 5% 需重点审查。
2. 累计变更：同一合同所有变更金额合计 / contract_amount > 15% 属于重大变更。
3. 签证时效：现场签证应在发生后 7 日内提出，超期签证有效性存疑。
4. 审批程序：变更金额超过50万元需建设单位、监理、施工三方签字确认。
5. 设计变更需原设计单位出具变更文件。

---

domain: 工程投资
type: BUSINESS_RULE

工程超概算审计规则：
1. 超概判定：actual_investment > approved_investment（实际完成投资超过批复概算）。
2. 超概率 = overbudget_amount / approved_investment * 100%。
3. 超概5%以内为一般超概，5%-10%为较大超概，超过10%为重大超概。
4. 重大超概需要重新报批调整概算。
5. 超概原因分析：区分价格上涨、设计变更、范围扩大等因素。

---

domain: 工程投资
type: BUSINESS_RULE

工程款支付审计规则：
1. 预付款比例：一般不超过合同金额的30%。
2. 进度款支付：按实际完成工程量核定，累计支付不超过合同金额的80%。
3. 质保金：通常按结算价的3%-5%预留，缺陷责任期满后退还。
4. 超付判定：SUM(paid_amount) > contract_amount，即累计支付超过合同金额。
5. 支付与计量一致性：paid_amount 应与 approved_amount（审批金额）匹配。

---

domain: 工程投资
type: TERM_MAPPING

总投资 → total_investment (project_info表)
政府投资 → govt_investment (project_info表)
自筹资金 → self_fund (project_info表)
合同金额 → contract_amount (project_contract表)
结算金额 → settlement_amount (project_contract表)
变更金额 → change_amount (project_change表)
支付金额 → paid_amount (project_payment表)
申请金额 → apply_amount (project_payment表)
审批金额 → approved_amount (project_payment表)
进度款 → payment_type = '进度款' (project_payment表)
预付款 → payment_type = '预付款' (project_payment表)
质保金 → payment_type = '质保金' (project_payment表)
批复投资 → approved_investment (project_settlement表)
实际投资 → actual_investment (project_settlement表)
超概金额 → overbudget_amount (project_settlement表)
超概率 → overbudget_rate (project_settlement表)
审减金额 → audit_reduce_amount (project_settlement表)
建设单位 → build_unit (project_info表)
承包单位 → contractor (project_contract表)
施工合同 → contract_type = '施工'
设计变更 → change_type = '设计变更'
现场签证 → change_type = '现场签证'
招标 → is_bidding = '是'
公开招标 → bidding_method = '公开招标'
