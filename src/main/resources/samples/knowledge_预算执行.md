domain: 预算执行
type: SQL_EXAMPLE
question: 查询各部门预算执行率排名

SELECT dept_name, budget_subject,
       budget_amount, executed_amount,
       ROUND(executed_amount / budget_amount * 100, 2) AS execution_rate
FROM budget_execution
WHERE fiscal_year = 2024 AND fiscal_month = 12
ORDER BY execution_rate ASC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 查询预算执行率低于50%的部门

SELECT dept_name,
       SUM(budget_amount) AS total_budget,
       SUM(executed_amount) AS total_executed,
       ROUND(SUM(executed_amount) / SUM(budget_amount) * 100, 2) AS exec_rate
FROM budget_execution
WHERE fiscal_year = 2024 AND fiscal_month = 12
GROUP BY dept_code, dept_name
HAVING exec_rate < 50
ORDER BY exec_rate ASC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 统计各月财政收入趋势

SELECT fiscal_month,
       SUM(actual_amount) AS monthly_revenue,
       SUM(plan_amount) AS monthly_plan,
       ROUND(SUM(actual_amount) / SUM(plan_amount) * 100, 2) AS completion_rate
FROM fiscal_revenue
WHERE fiscal_year = 2024
GROUP BY fiscal_month
ORDER BY fiscal_month;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 查询专项资金结余率超过30%的项目

SELECT fund_name, dept_name,
       approved_amount, allocated_amount, used_amount, balance_amount,
       ROUND(balance_amount / approved_amount * 100, 2) AS balance_rate
FROM special_fund
WHERE fiscal_year = 2024 AND approved_amount > 0
HAVING balance_rate > 30
ORDER BY balance_rate DESC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 查询三公经费超预算的部门

SELECT dept_name, expense_type,
       budget_amount, actual_amount,
       (actual_amount - budget_amount) AS over_amount,
       ROUND((actual_amount - budget_amount) / budget_amount * 100, 2) AS over_rate
FROM three_public_expense
WHERE fiscal_year = 2024 AND actual_amount > budget_amount
ORDER BY over_amount DESC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 查询各部门公务接待费用明细

SELECT dept_name, budget_amount, actual_amount,
       event_count, person_count,
       ROUND(actual_amount / event_count, 2) AS avg_per_event,
       detail_desc
FROM three_public_expense
WHERE fiscal_year = 2024 AND expense_type = '公务接待'
ORDER BY actual_amount DESC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 查询年度预算调整幅度超过20%的科目

SELECT dept_name, budget_subject, subject_code,
       bi.budget_amount AS original_budget,
       bi.adjusted_amount,
       ROUND((bi.adjusted_amount - bi.budget_amount) / bi.budget_amount * 100, 2) AS adjust_rate
FROM budget_indicator bi
WHERE bi.fiscal_year = 2024
  AND bi.adjusted_amount > 0
  AND ABS(bi.adjusted_amount - bi.budget_amount) / bi.budget_amount > 0.2
ORDER BY ABS(adjust_rate) DESC;

---

domain: 预算执行
type: SQL_EXAMPLE
question: 统计各地区税收收入和非税收入占比

SELECT region_name, revenue_type,
       SUM(actual_amount) AS total_amount
FROM fiscal_revenue
WHERE fiscal_year = 2024
GROUP BY region_code, region_name, revenue_type
ORDER BY region_name, revenue_type;

---

domain: 预算执行
type: GENERAL_DOC

预算执行审计主要内容：
1. 预算编制审计：审查预算编制是否完整、科学、合规，是否存在少编、漏编、虚编预算。
2. 预算执行审计：审查预算执行进度，关注执行率偏低或年末突击花钱。
3. 预算调整审计：审查预算调整是否经过法定程序批准，调整幅度是否合理。
4. 转移支付审计：审查上级转移支付资金是否及时下达、合规使用。
5. 专项资金审计：审查专项资金是否专款专用，有无挤占、挪用、截留。
6. 三公经费审计：审查因公出国、公务接待、公务用车费用是否控制在预算范围内。
7. 结转结余审计：审查结转结余资金是否按规定管理和使用。

---

domain: 预算执行
type: GENERAL_DOC

预算执行率分析说明：
预算执行率 = 已执行金额 / 年初预算金额 × 100%
- 执行率低于50%：预算执行进度严重偏慢，需重点关注原因。
- 执行率50%-80%：执行进度偏慢，应督促加快执行。
- 执行率80%-100%：预算执行正常。
- 执行率超过100%：存在超预算支出，需审查是否经过合规调整。

年末突击花钱判定标准：
- 第四季度（10-12月）支出占全年的比例超过40%
- 12月单月支出占全年比例超过20%

---

domain: 预算执行
type: BUSINESS_RULE

三公经费超预算审计规则：
1. 超预算判定：actual_amount > budget_amount，即实际支出超过预算金额。
2. 超预算率 = (actual_amount - budget_amount) / budget_amount * 100%。
3. 人均接待费标准：公务接待 actual_amount / person_count 不应超过当地规定标准（通常120-200元/人次）。
4. 公务用车审查：检查是否存在超编制配车、豪华配置等问题。
5. 因公出国审查：审查出国团组人数、天数、费用是否符合标准。

---

domain: 预算执行
type: BUSINESS_RULE

专项资金管理审计规则：
1. 资金拨付及时性：approved_amount 批复后30日内 allocated_amount 应完成首笔拨付。
2. 资金使用合规性：used_amount 用途应与 fund_purpose 一致，不得挪用。
3. 资金结余管理：balance_amount / approved_amount > 30% 为高结余，需关注。
4. 闲置资金判定：拨付超过1年且 used_amount / allocated_amount < 50%。
5. 重复申报：同一项目不同年度重复申请，需交叉比对 fund_name 和 recipient。

---

domain: 预算执行
type: BUSINESS_RULE

预算调整审计规则：
1. 调增幅度超过20%需重点审查：ABS(adjusted_amount - budget_amount) / budget_amount > 0.2。
2. 调整是否有合法审批文件。
3. 频繁调整：同一科目年度内调整超过2次应关注。
4. 预算追加资金来源是否合规。

---

domain: 预算执行
type: TERM_MAPPING

预算金额 → budget_amount (budget_indicator表/budget_execution表)
执行金额 → executed_amount (budget_execution表)
执行率 → execution_rate 或 executed_amount / budget_amount * 100
调整后金额 → adjusted_amount (budget_indicator表)
财政收入 → actual_amount (fiscal_revenue表)
税收收入 → revenue_type = '税收收入' (fiscal_revenue表)
非税收入 → revenue_type = '非税收入' (fiscal_revenue表)
专项资金 → special_fund表
拨付金额 → allocated_amount (special_fund表)
使用金额 → used_amount (special_fund表)
结余金额 → balance_amount (special_fund表)
三公经费 → three_public_expense表
公务接待 → expense_type = '公务接待'
公务用车 → expense_type = '公务用车'
因公出国 → expense_type = '因公出国'
接待次数 → event_count (three_public_expense表)
接待人次 → person_count (three_public_expense表)
部门编码 → dept_code
部门名称 → dept_name
预算科目 → budget_subject
科目编码 → subject_code
