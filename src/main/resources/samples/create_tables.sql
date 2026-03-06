-- ============================================================
-- 自然资源 领域建表
-- ============================================================

-- 土地利用现状表
CREATE TABLE land_use (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    region_code VARCHAR(20) NOT NULL COMMENT '行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '行政区划名称',
    land_type VARCHAR(50) NOT NULL COMMENT '土地类型（耕地/林地/草地/水域/建设用地/未利用地）',
    area_hectare DECIMAL(14,4) NOT NULL COMMENT '面积(公顷)',
    survey_year INT NOT NULL COMMENT '调查年度',
    data_source VARCHAR(100) COMMENT '数据来源',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region (region_code),
    INDEX idx_year (survey_year),
    INDEX idx_type (land_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='土地利用现状表';

-- 矿产资源登记表
CREATE TABLE mineral_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    mine_name VARCHAR(200) NOT NULL COMMENT '矿山名称',
    mine_code VARCHAR(50) COMMENT '采矿许可证号',
    mineral_type VARCHAR(50) NOT NULL COMMENT '矿种（煤/铁/铜/金/石灰�ite等）',
    region_code VARCHAR(20) NOT NULL COMMENT '所在行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '所在行政区划名称',
    mining_area DECIMAL(14,4) COMMENT '矿区面积(公顷)',
    reserve_amount DECIMAL(18,4) COMMENT '保有储量(万吨)',
    annual_output DECIMAL(14,4) COMMENT '年产量(万吨)',
    license_start DATE COMMENT '许可证有效期起',
    license_end DATE COMMENT '许可证有效期止',
    mine_status VARCHAR(20) DEFAULT '在采' COMMENT '矿山状态（在采/停采/关闭/基建）',
    enterprise_name VARCHAR(200) COMMENT '开采企业名称',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_mineral_type (mineral_type),
    INDEX idx_region (region_code),
    INDEX idx_status (mine_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='矿产资源登记表';

-- 土地出让收支表
CREATE TABLE land_transfer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    transfer_code VARCHAR(50) NOT NULL COMMENT '出让编号',
    region_code VARCHAR(20) NOT NULL COMMENT '行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '行政区划名称',
    land_location VARCHAR(300) COMMENT '地块位置',
    land_use_type VARCHAR(50) NOT NULL COMMENT '用途（商业/住宅/工业/综合）',
    transfer_method VARCHAR(20) NOT NULL COMMENT '出让方式（招标/拍卖/挂牌/协议）',
    transfer_area DECIMAL(14,4) COMMENT '出让面积(公顷)',
    transfer_price DECIMAL(18,2) NOT NULL COMMENT '出让价款(万元)',
    actual_paid DECIMAL(18,2) DEFAULT 0 COMMENT '实际缴纳金额(万元)',
    transfer_date DATE NOT NULL COMMENT '出让日期',
    buyer_name VARCHAR(200) COMMENT '受让方名称',
    contract_status VARCHAR(20) DEFAULT '正常' COMMENT '合同状态（正常/欠缴/违约）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region (region_code),
    INDEX idx_date (transfer_date),
    INDEX idx_use_type (land_use_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='土地出让收支表';

-- 水资源使用表
CREATE TABLE water_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    region_code VARCHAR(20) NOT NULL COMMENT '行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '行政区划名称',
    water_source_type VARCHAR(50) NOT NULL COMMENT '水源类型（地表水/地下水）',
    usage_type VARCHAR(50) NOT NULL COMMENT '用水类型（农业/工业/生活/生态）',
    permit_volume DECIMAL(14,4) COMMENT '许可取水量(万立方米)',
    actual_volume DECIMAL(14,4) COMMENT '实际取水量(万立方米)',
    report_year INT NOT NULL COMMENT '报告年度',
    enterprise_name VARCHAR(200) COMMENT '取水单位',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region (region_code),
    INDEX idx_year (report_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水资源使用表';

-- 生态保护红线表
CREATE TABLE eco_redline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    region_code VARCHAR(20) NOT NULL COMMENT '行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '行政区划名称',
    redline_type VARCHAR(50) NOT NULL COMMENT '红线类型（生物多样性/水源涵养/水土保持/防风固沙）',
    area_hectare DECIMAL(14,4) NOT NULL COMMENT '保护面积(公顷)',
    violation_count INT DEFAULT 0 COMMENT '违规占用次数',
    violation_area DECIMAL(14,4) DEFAULT 0 COMMENT '违规占用面积(公顷)',
    check_year INT NOT NULL COMMENT '检查年度',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region (region_code),
    INDEX idx_year (check_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生态保护红线表';


-- ============================================================
-- 预算执行 领域建表
-- ============================================================

-- 预算指标表
CREATE TABLE budget_indicator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fiscal_year INT NOT NULL COMMENT '财政年度',
    dept_code VARCHAR(20) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    budget_type VARCHAR(50) NOT NULL COMMENT '预算类型（一般公共预算/政府性基金/国有资本经营）',
    fund_source VARCHAR(50) COMMENT '资金来源（中央/省级/市级/县级）',
    budget_subject VARCHAR(100) NOT NULL COMMENT '预算科目',
    subject_code VARCHAR(30) COMMENT '科目编码',
    budget_amount DECIMAL(18,2) NOT NULL COMMENT '预算金额(万元)',
    adjusted_amount DECIMAL(18,2) DEFAULT 0 COMMENT '调整后金额(万元)',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_year (fiscal_year),
    INDEX idx_dept (dept_code),
    INDEX idx_subject (subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算指标表';

-- 预算执行明细表
CREATE TABLE budget_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fiscal_year INT NOT NULL COMMENT '财政年度',
    fiscal_month INT NOT NULL COMMENT '月份(1-12)',
    dept_code VARCHAR(20) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    budget_subject VARCHAR(100) NOT NULL COMMENT '预算科目',
    subject_code VARCHAR(30) COMMENT '科目编码',
    budget_amount DECIMAL(18,2) NOT NULL COMMENT '年初预算金额(万元)',
    executed_amount DECIMAL(18,2) NOT NULL COMMENT '已执行金额(万元)',
    payment_amount DECIMAL(18,2) DEFAULT 0 COMMENT '本月支付金额(万元)',
    execution_rate DECIMAL(5,2) COMMENT '执行率(%)',
    voucher_no VARCHAR(50) COMMENT '凭证号',
    payee VARCHAR(200) COMMENT '收款方',
    purpose VARCHAR(500) COMMENT '用途说明',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_year_month (fiscal_year, fiscal_month),
    INDEX idx_dept (dept_code),
    INDEX idx_subject (subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算执行明细表';

-- 财政收入表
CREATE TABLE fiscal_revenue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fiscal_year INT NOT NULL COMMENT '财政年度',
    fiscal_month INT NOT NULL COMMENT '月份(1-12)',
    region_code VARCHAR(20) NOT NULL COMMENT '行政区划代码',
    region_name VARCHAR(100) NOT NULL COMMENT '行政区划名称',
    revenue_type VARCHAR(50) NOT NULL COMMENT '收入类型（税收收入/非税收入）',
    revenue_subject VARCHAR(100) NOT NULL COMMENT '收入科目',
    subject_code VARCHAR(30) COMMENT '科目编码',
    plan_amount DECIMAL(18,2) COMMENT '计划收入(万元)',
    actual_amount DECIMAL(18,2) NOT NULL COMMENT '实际收入(万元)',
    yoy_growth DECIMAL(8,2) COMMENT '同比增长率(%)',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_year_month (fiscal_year, fiscal_month),
    INDEX idx_region (region_code),
    INDEX idx_type (revenue_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财政收入表';

-- 专项资金拨付表
CREATE TABLE special_fund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fiscal_year INT NOT NULL COMMENT '财政年度',
    fund_name VARCHAR(200) NOT NULL COMMENT '专项资金名称',
    fund_code VARCHAR(50) COMMENT '资金编码',
    dept_code VARCHAR(20) NOT NULL COMMENT '主管部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '主管部门名称',
    approved_amount DECIMAL(18,2) NOT NULL COMMENT '批复金额(万元)',
    allocated_amount DECIMAL(18,2) DEFAULT 0 COMMENT '已拨付金额(万元)',
    used_amount DECIMAL(18,2) DEFAULT 0 COMMENT '已使用金额(万元)',
    balance_amount DECIMAL(18,2) DEFAULT 0 COMMENT '结余金额(万元)',
    allocate_date DATE COMMENT '拨付日期',
    recipient VARCHAR(200) COMMENT '接收单位',
    fund_purpose VARCHAR(500) COMMENT '资金用途',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_year (fiscal_year),
    INDEX idx_dept (dept_code),
    INDEX idx_fund (fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专项资金拨付表';

-- 三公经费明细表
CREATE TABLE three_public_expense (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fiscal_year INT NOT NULL COMMENT '财政年度',
    dept_code VARCHAR(20) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    expense_type VARCHAR(50) NOT NULL COMMENT '经费类型（因公出国/公务用车/公务接待）',
    budget_amount DECIMAL(18,2) NOT NULL COMMENT '预算金额(万元)',
    actual_amount DECIMAL(18,2) DEFAULT 0 COMMENT '实际支出(万元)',
    event_count INT DEFAULT 0 COMMENT '发生次数/批次',
    person_count INT DEFAULT 0 COMMENT '涉及人次',
    detail_desc VARCHAR(500) COMMENT '明细说明',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_year (fiscal_year),
    INDEX idx_dept (dept_code),
    INDEX idx_type (expense_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三公经费明细表';


-- ============================================================
-- 工程投资 领域建表
-- ============================================================

-- 工程项目基本信息表
CREATE TABLE project_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) NOT NULL COMMENT '项目编码',
    project_name VARCHAR(300) NOT NULL COMMENT '项目名称',
    project_type VARCHAR(50) NOT NULL COMMENT '项目类型（交通/水利/市政/房建/能源）',
    region_code VARCHAR(20) COMMENT '所在行政区划代码',
    region_name VARCHAR(100) COMMENT '所在行政区划名称',
    build_unit VARCHAR(200) NOT NULL COMMENT '建设单位',
    total_investment DECIMAL(18,2) NOT NULL COMMENT '总投资(万元)',
    govt_investment DECIMAL(18,2) DEFAULT 0 COMMENT '政府投资(万元)',
    self_fund DECIMAL(18,2) DEFAULT 0 COMMENT '自筹资金(万元)',
    bank_loan DECIMAL(18,2) DEFAULT 0 COMMENT '银行贷款(万元)',
    plan_start_date DATE COMMENT '计划开工日期',
    plan_end_date DATE COMMENT '计划竣工日期',
    actual_start_date DATE COMMENT '实际开工日期',
    actual_end_date DATE COMMENT '实际竣工日期',
    project_status VARCHAR(20) DEFAULT '在建' COMMENT '项目状态（前期/在建/竣工/停工）',
    approval_doc VARCHAR(200) COMMENT '立项批复文号',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_project_code (project_code),
    INDEX idx_type (project_type),
    INDEX idx_status (project_status),
    INDEX idx_build_unit (build_unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程项目基本信息表';

-- 工程合同表
CREATE TABLE project_contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) NOT NULL COMMENT '项目编码',
    contract_code VARCHAR(50) NOT NULL COMMENT '合同编号',
    contract_name VARCHAR(300) NOT NULL COMMENT '合同名称',
    contract_type VARCHAR(50) NOT NULL COMMENT '合同类型（施工/监理/设计/勘察/采购）',
    contractor VARCHAR(200) NOT NULL COMMENT '承包/供应单位',
    contract_amount DECIMAL(18,2) NOT NULL COMMENT '合同金额(万元)',
    signed_date DATE COMMENT '签订日期',
    start_date DATE COMMENT '合同开始日期',
    end_date DATE COMMENT '合同结束日期',
    settlement_amount DECIMAL(18,2) COMMENT '结算金额(万元)',
    is_bidding VARCHAR(10) DEFAULT '是' COMMENT '是否经过招投标(是/否)',
    bidding_method VARCHAR(30) COMMENT '招标方式（公开招标/邀请招标/竞争性谈判/单一来源）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project (project_code),
    INDEX idx_contract (contract_code),
    INDEX idx_contractor (contractor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程合同表';

-- 工程计量支付表
CREATE TABLE project_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) NOT NULL COMMENT '项目编码',
    contract_code VARCHAR(50) NOT NULL COMMENT '合同编号',
    payment_no VARCHAR(50) NOT NULL COMMENT '支付编号',
    payment_period VARCHAR(50) COMMENT '计量期次',
    apply_amount DECIMAL(18,2) NOT NULL COMMENT '申请金额(万元)',
    approved_amount DECIMAL(18,2) COMMENT '审批金额(万元)',
    paid_amount DECIMAL(18,2) DEFAULT 0 COMMENT '实际支付金额(万元)',
    payment_date DATE COMMENT '支付日期',
    payment_type VARCHAR(30) COMMENT '支付类型（进度款/预付款/质保金/结算款）',
    reviewer VARCHAR(100) COMMENT '审核人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project (project_code),
    INDEX idx_contract (contract_code),
    INDEX idx_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程计量支付表';

-- 工程变更签证表
CREATE TABLE project_change (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) NOT NULL COMMENT '项目编码',
    contract_code VARCHAR(50) COMMENT '合同编号',
    change_code VARCHAR(50) NOT NULL COMMENT '变更编号',
    change_type VARCHAR(50) NOT NULL COMMENT '变更类型（设计变更/现场签证/工程量变更）',
    change_reason VARCHAR(500) COMMENT '变更原因',
    change_content VARCHAR(1000) COMMENT '变更内容',
    change_amount DECIMAL(18,2) NOT NULL COMMENT '变更金额(万元)',
    apply_date DATE COMMENT '申请日期',
    approve_date DATE COMMENT '审批日期',
    approve_status VARCHAR(20) DEFAULT '待审批' COMMENT '审批状态（待审批/已批准/已驳回）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project (project_code),
    INDEX idx_type (change_type),
    INDEX idx_status (approve_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程变更签证表';

-- 竣工决算表
CREATE TABLE project_settlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(50) NOT NULL COMMENT '项目编码',
    project_name VARCHAR(300) NOT NULL COMMENT '项目名称',
    approved_investment DECIMAL(18,2) NOT NULL COMMENT '批复总投资(万元)',
    actual_investment DECIMAL(18,2) NOT NULL COMMENT '实际完成投资(万元)',
    construction_cost DECIMAL(18,2) COMMENT '建筑安装工程费(万元)',
    equipment_cost DECIMAL(18,2) COMMENT '设备购置费(万元)',
    other_cost DECIMAL(18,2) COMMENT '其他费用(万元)',
    overbudget_amount DECIMAL(18,2) COMMENT '超概金额(万元)',
    overbudget_rate DECIMAL(8,2) COMMENT '超概率(%)',
    settlement_date DATE COMMENT '决算日期',
    audit_status VARCHAR(20) DEFAULT '待审计' COMMENT '审计状态（待审计/审计中/已完成）',
    audit_reduce_amount DECIMAL(18,2) COMMENT '审减金额(万元)',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_project (project_code),
    INDEX idx_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竣工决算表';
