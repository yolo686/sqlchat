domain: 自然资源
type: SQL_EXAMPLE
question: 查询各地区耕地面积排名

SELECT region_name, SUM(area_hectare) AS total_area
FROM land_use
WHERE land_type = '耕地' AND survey_year = 2024
GROUP BY region_code, region_name
ORDER BY total_area DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 查询土地出让收入欠缴情况

SELECT region_name, transfer_code, land_location, land_use_type,
       transfer_price, actual_paid,
       (transfer_price - actual_paid) AS unpaid_amount,
       contract_status
FROM land_transfer
WHERE actual_paid < transfer_price AND contract_status = '欠缴'
ORDER BY (transfer_price - actual_paid) DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 统计各类型矿山数量和年产量

SELECT mineral_type,
       COUNT(*) AS mine_count,
       SUM(annual_output) AS total_output,
       SUM(reserve_amount) AS total_reserve
FROM mineral_resource
WHERE mine_status = '在采'
GROUP BY mineral_type
ORDER BY mine_count DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 查询过期未注销的采矿许可证

SELECT mine_name, mine_code, mineral_type, region_name,
       enterprise_name, license_start, license_end, mine_status
FROM mineral_resource
WHERE license_end < CURDATE() AND mine_status NOT IN ('关闭')
ORDER BY license_end ASC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 查询某年度超量取水的企业

SELECT enterprise_name, region_name, water_source_type,
       permit_volume, actual_volume,
       (actual_volume - permit_volume) AS over_volume,
       ROUND((actual_volume - permit_volume) / permit_volume * 100, 2) AS over_rate
FROM water_resource
WHERE report_year = 2024 AND actual_volume > permit_volume
ORDER BY over_volume DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 统计各地区土地出让收入总额及出让面积

SELECT region_name,
       COUNT(*) AS transfer_count,
       SUM(transfer_area) AS total_area,
       SUM(transfer_price) AS total_price,
       SUM(actual_paid) AS total_paid
FROM land_transfer
WHERE YEAR(transfer_date) = 2024
GROUP BY region_code, region_name
ORDER BY total_price DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 查询生态保护红线违规占用情况

SELECT region_name, redline_type,
       area_hectare AS protection_area,
       violation_count, violation_area,
       ROUND(violation_area / area_hectare * 100, 4) AS violation_rate
FROM eco_redline
WHERE check_year = 2024 AND violation_count > 0
ORDER BY violation_area DESC;

---

domain: 自然资源
type: SQL_EXAMPLE
question: 查询协议出让土地中低价出让的地块

SELECT region_name, transfer_code, land_location, land_use_type,
       transfer_area, transfer_price,
       ROUND(transfer_price / transfer_area, 2) AS unit_price
FROM land_transfer
WHERE transfer_method = '协议'
ORDER BY unit_price ASC
LIMIT 20;

---

domain: 自然资源
type: GENERAL_DOC

自然资源审计主要涵盖以下领域：
1. 土地资源管理审计：审查土地利用规划执行、土地出让收支、征地补偿、闲置土地处置等情况。
2. 矿产资源审计：审查矿业权出让收益、矿山开采许可管理、越界开采、资源税费征缴等情况。
3. 水资源审计：审查取水许可管理、水资源费征缴、水功能区管理、水土保持等情况。
4. 生态保护审计：审查生态保护红线落实、自然保护区管理、生态修复资金使用等情况。
5. 森林草原资源审计：审查林权管理、林业生态建设资金、草原禁牧休牧制度执行等情况。

常见审计关注点：
- 土地出让收入是否应收尽收
- 矿业权出让是否规范、收益是否足额征收
- 是否存在非法占用耕地、林地的情况
- 生态修复资金是否专款专用
- 自然资源资产是否如实入账

---

domain: 自然资源
type: GENERAL_DOC

土地出让制度概述：
1. 出让方式：
   - 招标出让：适用于经营性用地和政策规定需要招标的项目
   - 拍卖出让：由出价最高者获得土地使用权
   - 挂牌出让：在规定期限内接受报价，到期确定竞得人
   - 协议出让：适用于工业用地等特定情形
2. 出让金管理：
   - 土地出让收入实行收支两条线管理
   - 必须全额缴入国库，纳入地方基金预算管理
   - 不得以任何形式减免、返还土地出让金
3. 审计重点：
   - 出让方式是否合规
   - 出让底价是否经过评估
   - 出让金是否按期足额缴纳
   - 是否存在先用后批、未批先用等违规行为

---

domain: 自然资源
type: BUSINESS_RULE

土地出让欠缴审计规则：
合同约定价款 transfer_price 与实际缴纳金额 actual_paid 差额即为欠缴金额。
判定条件：actual_paid < transfer_price，且 contract_status 为"欠缴"或"违约"。
欠缴率 = (transfer_price - actual_paid) / transfer_price * 100%。
超过合同约定期限180天仍未缴齐的应重点关注。

---

domain: 自然资源
type: BUSINESS_RULE

矿产资源采矿许可证审计规则：
1. 许可证到期未注销：license_end < 当前日期 且 mine_status 不为"关闭"，表示许可证过期仍在运营。
2. 越界开采判定：实际开采面积超过 mining_area 许可面积。
3. 超量开采：annual_output 超过许可开采量，需查核年检报告。
4. 无证开采：mine_code 为空或无有效记录，但 mine_status 为"在采"。

---

domain: 自然资源
type: BUSINESS_RULE

生态保护红线违规占用审计规则：
1. 违规占用判定：violation_count > 0 或 violation_area > 0。
2. 严重违规：violation_area / area_hectare > 0.01（占保护面积1%以上）。
3. 重点关注区域：redline_type 为"生物多样性"和"水源涵养"类型。
4. 需追踪整改：检查前一年度发现的违规是否已恢复原状。

---

domain: 自然资源
type: TERM_MAPPING

出让面积 → transfer_area (land_transfer表)
出让价款 → transfer_price (land_transfer表)
已缴金额 → actual_paid (land_transfer表)
欠缴金额 → transfer_price - actual_paid
矿山名称 → mine_name (mineral_resource表)
采矿许可证号 → mine_code (mineral_resource表)
矿种 → mineral_type (mineral_resource表)
保有储量 → reserve_amount (mineral_resource表)
年产量 → annual_output (mineral_resource表)
耕地 → land_type = '耕地' (land_use表)
建设用地 → land_type = '建设用地' (land_use表)
取水量 → actual_volume (water_resource表)
许可取水量 → permit_volume (water_resource表)
红线面积 → area_hectare (eco_redline表)
违规面积 → violation_area (eco_redline表)
行政区划代码 → region_code
行政区划名称 → region_name
