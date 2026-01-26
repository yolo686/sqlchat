##
提问：查询所有耕地的信息
SQL：SELECT * FROM land_resource WHERE land_type = '耕地';
##
提问：统计广东省的土地总面积
SQL：SELECT SUM(area) AS total_area FROM land_resource WHERE region LIKE '广东省%';
##
提问：找出审计状态为 “存在问题” 的土地编码和所属地区
SQL：SELECT land_code, region FROM land_resource WHERE audit_status = '存在问题';
##
提问：查看 2023 年审批的土地有哪些
SQL：SELECT * FROM land_resource WHERE approval_date BETWEEN '2023-01-01' AND '2023-12-31';
##
提问：统计不同土地类型的数量
SQL：SELECT land_type, COUNT(*) AS count FROM land_resource GROUP BY land_type;
##
提问：查询违规占用的土地对应的违规类型和整改状态
SQL：SELECT lr.land_code, lr.region, lv.violation_type, lv.rectification_status FROM land_resource lr JOIN land_violation lv ON lr.land_id = lv.land_id WHERE lr.use_status = '违规占用';
##
提问：统计每个行政区的违规用地总面积
SQL：SELECT lr.region, SUM(lv.violation_area) AS total_violation_area FROM land_resource lr JOIN land_violation lv ON lr.land_id = lv.land_id GROUP BY lr.region;
##
提问：找出未整改且处罚金额超过 5 万元的违规记录，显示土地编码和审计人员
SQL：SELECT lr.land_code, lv.auditor FROM land_resource lr JOIN land_violation lv ON lr.land_id = lv.land_id WHERE lv.rectification_status = '未整改' AND lv.penalty_amount > 50000;
##
提问：计算 2023 年各省份违规用地的处罚总金额，并按金额降序排列
SQL：SELECT SUBSTRING(lr.region, 1, 2) AS province, SUM(lv.penalty_amount) AS total_penalty FROM land_resource lr JOIN land_violation lv ON lr.land_id = lv.land_id WHERE lv.violation_date BETWEEN '2023-01-01' AND '2023-12-31' GROUP BY province ORDER BY total_penalty DESC;
##
提问：找出超审批开采的矿产记录，显示矿产类型、区域和超采量（开采量 - 审批量）
SQL：SELECT mineral_type, region, (mining_volume - approval_volume) AS over_mining_volume FROM mineral_mining WHERE mining_volume > approval_volume;
##
提问：统计审计状态为 “已审计” 的耕地中，合规使用的占比
SQL：SELECT CONCAT(ROUND(COUNT(CASE WHEN use_status = '合规使用' THEN 1 END) / COUNT(*) * 100, 2), '%') AS compliance_rate FROM land_resource WHERE land_type = '耕地' AND audit_status = '已审计';
##
提问：查询违规类型为 “改变用途” 且整改状态为 “未整改” 的林地信息，包括土地面积和所属地区
SQL：SELECT lr.land_code, lr.area, lr.region FROM land_resource lr JOIN land_violation lv ON lr.land_id = lv.land_id WHERE lr.land_type = '林地' AND lv.violation_type = '改变用途' AND lv.rectification_status = '未整改';