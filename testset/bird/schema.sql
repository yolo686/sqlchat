-- ============================================================
-- Bird 风格测试数据库 (5个独立Schema，偏业务复杂场景)
-- ============================================================

-- ==================== Schema 1: financial ====================
DROP TABLE IF EXISTS loan_payments;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS branches;

CREATE TABLE branches (
    branch_id INT PRIMARY KEY AUTO_INCREMENT,
    branch_name VARCHAR(100) NOT NULL,
    city VARCHAR(50),
    region VARCHAR(50),
    open_date DATE
) COMMENT='银行支行';

CREATE TABLE accounts (
    account_id INT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(30) UNIQUE NOT NULL,
    holder_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL COMMENT '储蓄/活期/定期',
    balance DECIMAL(14,2) DEFAULT 0,
    branch_id INT,
    open_date DATE,
    status VARCHAR(10) DEFAULT 'active',
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
) COMMENT='银行账户';

CREATE TABLE transactions (
    txn_id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    txn_date DATE NOT NULL,
    txn_type VARCHAR(20) NOT NULL COMMENT '存款/取款/转账/消费',
    amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(20) COMMENT '柜台/ATM/网银/手机',
    counterparty VARCHAR(100),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) COMMENT='交易流水';

CREATE TABLE loans (
    loan_id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    loan_type VARCHAR(30) NOT NULL COMMENT '房贷/车贷/消费贷/经营贷',
    principal DECIMAL(14,2) NOT NULL,
    interest_rate DECIMAL(5,3) NOT NULL,
    term_months INT NOT NULL,
    start_date DATE,
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/paid_off/overdue',
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) COMMENT='贷款表';

CREATE TABLE loan_payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    loan_id INT NOT NULL,
    payment_date DATE NOT NULL,
    payment_amount DECIMAL(12,2) NOT NULL,
    principal_paid DECIMAL(12,2),
    interest_paid DECIMAL(12,2),
    remaining_balance DECIMAL(14,2),
    is_overdue TINYINT DEFAULT 0,
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id)
) COMMENT='贷款还款记录';

INSERT INTO branches VALUES
(1,'城中支行','杭州','华东','2010-01-01'),(2,'高新支行','杭州','华东','2015-06-01'),
(3,'浦东支行','上海','华东','2008-03-15'),(4,'南山支行','深圳','华南','2012-09-01'),
(5,'天府支行','成都','西南','2018-01-01');

INSERT INTO accounts VALUES
(1,'A100001','张三','储蓄',85000.00,1,'2020-01-15','active'),
(2,'A100002','李四','活期',23000.00,1,'2021-03-20','active'),
(3,'A100003','王五','储蓄',156000.00,2,'2019-06-10','active'),
(4,'A100004','赵六','定期',500000.00,3,'2018-11-01','active'),
(5,'A100005','钱七','活期',12000.00,3,'2022-02-14','active'),
(6,'A100006','孙八','储蓄',45000.00,4,'2020-08-20','active'),
(7,'A100007','周九','活期',8000.00,4,'2023-01-10','active'),
(8,'A100008','吴十','储蓄',320000.00,5,'2019-04-01','active'),
(9,'A100009','郑十一','定期',200000.00,2,'2021-07-15','active'),
(10,'A100010','冯十二','活期',3500.00,5,'2023-06-01','inactive');

INSERT INTO transactions VALUES
(1,1,'2024-01-05','存款',20000.00,'柜台',NULL),(2,1,'2024-01-15','消费',3500.00,'手机','京东商城'),
(3,1,'2024-02-01','转账',10000.00,'网银','李四'),(4,2,'2024-01-10','存款',5000.00,'ATM',NULL),
(5,2,'2024-01-20','取款',2000.00,'ATM',NULL),(6,3,'2024-01-08','存款',30000.00,'柜台',NULL),
(7,3,'2024-02-10','消费',8500.00,'手机','天猫超市'),(8,4,'2024-01-12','存款',100000.00,'柜台',NULL),
(9,5,'2024-01-25','消费',1500.00,'手机','美团'),(10,5,'2024-02-05','取款',3000.00,'ATM',NULL),
(11,6,'2024-01-18','存款',15000.00,'网银',NULL),(12,6,'2024-02-15','转账',5000.00,'手机','王五'),
(13,7,'2024-02-01','消费',2800.00,'手机','淘宝'),(14,8,'2024-01-30','存款',50000.00,'柜台',NULL),
(15,8,'2024-02-20','消费',12000.00,'网银','携程旅游'),(16,9,'2024-01-05','存款',80000.00,'柜台',NULL),
(17,1,'2024-02-15','消费',4200.00,'手机','拼多多'),(18,3,'2024-02-28','消费',2300.00,'手机','滴滴出行'),
(19,2,'2024-02-10','消费',1800.00,'手机','饿了么'),(20,4,'2024-02-25','取款',50000.00,'柜台',NULL);

INSERT INTO loans VALUES
(1,1,'房贷',800000.00,3.850,360,'2020-03-01','active'),
(2,3,'车贷',200000.00,4.350,60,'2022-01-15','active'),
(3,4,'经营贷',500000.00,5.200,36,'2023-06-01','active'),
(4,6,'消费贷',50000.00,6.500,24,'2023-09-01','active'),
(5,8,'房贷',1200000.00,3.650,360,'2019-10-01','active'),
(6,2,'消费贷',30000.00,7.200,12,'2023-03-01','paid_off'),
(7,5,'消费贷',20000.00,8.000,12,'2023-08-01','overdue');

INSERT INTO loan_payments VALUES
(1,1,'2024-01-01',3800.00,1500.00,2300.00,785000.00,0),
(2,1,'2024-02-01',3800.00,1520.00,2280.00,783480.00,0),
(3,2,'2024-01-15',3800.00,3100.00,700.00,155000.00,0),
(4,2,'2024-02-15',3800.00,3120.00,680.00,151880.00,0),
(5,3,'2024-01-01',15500.00,13200.00,2300.00,410000.00,0),
(6,3,'2024-02-01',15500.00,13300.00,2200.00,396700.00,0),
(7,4,'2024-01-01',2400.00,2100.00,300.00,38000.00,0),
(8,5,'2024-01-01',5500.00,1800.00,3700.00,1165000.00,0),
(9,5,'2024-02-01',5500.00,1810.00,3690.00,1163190.00,0),
(10,7,'2024-01-01',1900.00,1700.00,200.00,10000.00,1),
(11,7,'2024-02-01',0.00,0.00,0.00,10000.00,1);

-- ==================== Schema 2: retail_analytics ====================
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS store_products;
DROP TABLE IF EXISTS stores;
DROP TABLE IF EXISTS retail_products;

CREATE TABLE stores (
    store_id INT PRIMARY KEY AUTO_INCREMENT,
    store_name VARCHAR(100) NOT NULL,
    city VARCHAR(50),
    province VARCHAR(50),
    store_type VARCHAR(20) COMMENT '旗舰店/标准店/社区店',
    open_date DATE,
    area_sqm DECIMAL(8,2) COMMENT '面积(平方米)'
) COMMENT='门店表';

CREATE TABLE retail_products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    subcategory VARCHAR(50),
    unit_cost DECIMAL(10,2),
    unit_price DECIMAL(10,2),
    supplier VARCHAR(100)
) COMMENT='商品主数据';

CREATE TABLE sales (
    sale_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL,
    product_id INT NOT NULL,
    sale_date DATE NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_rate DECIMAL(4,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores(store_id),
    FOREIGN KEY (product_id) REFERENCES retail_products(product_id)
) COMMENT='销售明细';

CREATE TABLE promotions (
    promo_id INT PRIMARY KEY AUTO_INCREMENT,
    promo_name VARCHAR(100),
    start_date DATE,
    end_date DATE,
    discount_pct DECIMAL(4,2),
    category VARCHAR(50),
    min_purchase DECIMAL(10,2) DEFAULT 0
) COMMENT='促销活动';

INSERT INTO stores VALUES
(1,'杭州旗舰店','杭州','浙江','旗舰店','2018-01-01',800.00),
(2,'杭州城西店','杭州','浙江','标准店','2020-03-15',350.00),
(3,'上海南京路店','上海','上海','旗舰店','2016-06-01',1200.00),
(4,'深圳福田店','深圳','广东','标准店','2019-09-01',400.00),
(5,'成都春熙路店','成都','四川','旗舰店','2021-01-01',600.00),
(6,'杭州社区店','杭州','浙江','社区店','2023-06-01',120.00);

INSERT INTO retail_products VALUES
(1,'有机牛奶','食品','乳制品',8.50,15.90,'蒙牛'),
(2,'进口橄榄油','食品','调味品',45.00,89.00,'贝蒂斯'),
(3,'洗衣液','日化','清洁用品',12.00,29.90,'蓝月亮'),
(4,'面巾纸','日化','纸品',6.00,12.90,'维达'),
(5,'矿泉水','饮料','水',1.20,2.50,'农夫山泉'),
(6,'可乐','饮料','碳酸饮料',1.80,3.50,'可口可乐'),
(7,'牛肉干','食品','零食',25.00,49.90,'科尔沁'),
(8,'洗发水','日化','洗护',18.00,39.90,'海飞丝');

INSERT INTO sales VALUES
(1,1,1,'2024-01-05',20,15.90,0,318.00),(2,1,2,'2024-01-05',5,89.00,0.10,400.50),
(3,1,3,'2024-01-08',15,29.90,0,448.50),(4,1,5,'2024-01-08',50,2.50,0,125.00),
(5,2,1,'2024-01-10',10,15.90,0,159.00),(6,2,4,'2024-01-10',30,12.90,0,387.00),
(7,3,2,'2024-01-12',8,89.00,0.15,605.20),(8,3,7,'2024-01-12',12,49.90,0,598.80),
(9,3,8,'2024-01-15',10,39.90,0,399.00),(10,4,1,'2024-01-15',15,15.90,0,238.50),
(11,4,6,'2024-01-18',40,3.50,0,140.00),(12,5,3,'2024-01-20',20,29.90,0.05,568.10),
(13,5,5,'2024-01-20',60,2.50,0,150.00),(14,1,1,'2024-02-01',25,15.90,0,397.50),
(15,1,7,'2024-02-01',8,49.90,0,399.20),(16,2,6,'2024-02-05',35,3.50,0,122.50),
(17,3,1,'2024-02-10',30,15.90,0,477.00),(18,3,3,'2024-02-10',18,29.90,0.10,484.38),
(19,4,8,'2024-02-15',6,39.90,0,239.40),(20,5,4,'2024-02-18',25,12.90,0,322.50),
(21,6,1,'2024-02-20',8,15.90,0,127.20),(22,6,5,'2024-02-20',20,2.50,0,50.00),
(23,1,8,'2024-02-25',12,39.90,0.05,455.46),(24,2,7,'2024-02-28',6,49.90,0,299.40);

INSERT INTO promotions VALUES
(1,'年货节',  '2024-01-01','2024-01-31',0.10,'食品',50.00),
(2,'春季清仓','2024-02-15','2024-03-15',0.15,'日化',30.00),
(3,'会员日',  '2024-01-15','2024-01-15',0.20,NULL,0);

-- ==================== Schema 3: school_district ====================
DROP TABLE IF EXISTS test_scores;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS school_students;
DROP TABLE IF EXISTS schools;

CREATE TABLE schools (
    school_id INT PRIMARY KEY AUTO_INCREMENT,
    school_name VARCHAR(100) NOT NULL,
    district VARCHAR(50),
    school_type VARCHAR(20) COMMENT '小学/初中/高中',
    student_capacity INT,
    teacher_count INT,
    founded_year INT
) COMMENT='学校表';

CREATE TABLE school_students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50),
    school_id INT,
    grade INT COMMENT '年级',
    class_name VARCHAR(10),
    gender VARCHAR(5),
    enrollment_year INT,
    FOREIGN KEY (school_id) REFERENCES schools(school_id)
) COMMENT='学生表';

CREATE TABLE teachers (
    teacher_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50),
    school_id INT,
    subject VARCHAR(30),
    title VARCHAR(20) COMMENT '初级/中级/高级/特级',
    years_teaching INT,
    FOREIGN KEY (school_id) REFERENCES schools(school_id)
) COMMENT='教师表';

CREATE TABLE test_scores (
    score_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    subject VARCHAR(30) NOT NULL,
    exam_type VARCHAR(20) COMMENT '期中/期末/月考',
    exam_date DATE,
    score DECIMAL(5,1) NOT NULL,
    full_marks DECIMAL(5,1) DEFAULT 100,
    FOREIGN KEY (student_id) REFERENCES school_students(student_id)
) COMMENT='考试成绩';

INSERT INTO schools VALUES
(1,'实验小学','西湖区','小学',1200,80,1985),
(2,'文澜中学','拱墅区','初中',1500,120,1995),
(3,'杭二中','上城区','高中',2000,200,1940),
(4,'求是小学','西湖区','小学',900,60,2005),
(5,'建兰中学','上城区','初中',1800,150,1990);

INSERT INTO school_students VALUES
(1,'小明',1,3,'3-1','M',2022),(2,'小红',1,3,'3-1','F',2022),(3,'小刚',1,5,'5-2','M',2020),
(4,'小丽',2,7,'7-3','F',2023),(5,'小华',2,8,'8-1','M',2022),(6,'小芳',2,9,'9-2','F',2021),
(7,'小强',3,10,'10-1','M',2024),(8,'小美',3,11,'11-3','F',2023),(9,'小伟',3,12,'12-1','M',2022),
(10,'小雪',4,2,'2-1','F',2023),(11,'小龙',4,4,'4-2','M',2021),(12,'小燕',5,7,'7-1','F',2023),
(13,'小磊',5,8,'8-2','M',2022),(14,'小琳',5,9,'9-1','F',2021),(15,'小杰',2,7,'7-3','M',2023);

INSERT INTO teachers VALUES
(1,'王老师',1,'语文','高级',20),(2,'李老师',1,'数学','中级',12),
(3,'张老师',2,'英语','高级',18),(4,'陈老师',2,'数学','特级',25),
(5,'刘老师',3,'物理','高级',15),(6,'赵老师',3,'化学','中级',10),
(7,'黄老师',4,'语文','中级',8),(8,'周老师',5,'数学','高级',22);

INSERT INTO test_scores VALUES
(1,1,'语文','期中','2024-04-15',92.0,100),(2,1,'数学','期中','2024-04-15',88.5,100),
(3,2,'语文','期中','2024-04-15',95.0,100),(4,2,'数学','期中','2024-04-15',91.0,100),
(5,3,'语文','期末','2024-01-10',85.0,100),(6,3,'数学','期末','2024-01-10',90.0,100),
(7,4,'语文','期中','2024-04-15',78.0,100),(8,4,'数学','期中','2024-04-15',82.0,100),
(9,4,'英语','期中','2024-04-15',88.0,100),(10,5,'语文','期中','2024-04-15',72.0,100),
(11,5,'数学','期中','2024-04-15',95.0,100),(12,5,'英语','期中','2024-04-15',80.0,100),
(13,6,'语文','期末','2024-01-10',90.0,100),(14,6,'数学','期末','2024-01-10',85.0,100),
(15,7,'语文','月考','2024-03-20',68.0,100),(16,7,'数学','月考','2024-03-20',75.0,100),
(17,8,'物理','期中','2024-04-15',88.0,100),(18,8,'化学','期中','2024-04-15',92.0,100),
(19,9,'物理','期末','2024-01-10',78.0,100),(20,9,'化学','期末','2024-01-10',81.0,100),
(21,10,'语文','期中','2024-04-15',96.0,100),(22,10,'数学','期中','2024-04-15',93.0,100),
(23,11,'语文','期末','2024-01-10',80.0,100),(24,11,'数学','期末','2024-01-10',76.0,100),
(25,12,'语文','期中','2024-04-15',85.0,100),(26,12,'数学','期中','2024-04-15',90.0,100),
(27,13,'语文','期中','2024-04-15',70.0,100),(28,13,'数学','期中','2024-04-15',88.0,100),
(29,14,'语文','期末','2024-01-10',92.0,100),(30,14,'数学','期末','2024-01-10',87.0,100),
(31,1,'语文','期末','2024-01-10',89.0,100),(32,1,'数学','期末','2024-01-10',91.0,100),
(33,15,'语文','期中','2024-04-15',74.0,100),(34,15,'数学','期中','2024-04-15',80.0,100);

-- ==================== Schema 4: property ====================
DROP TABLE IF EXISTS property_transactions;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS communities;

CREATE TABLE communities (
    community_id INT PRIMARY KEY AUTO_INCREMENT,
    community_name VARCHAR(100) NOT NULL,
    district VARCHAR(50),
    city VARCHAR(50),
    built_year INT,
    total_units INT,
    property_type VARCHAR(30) COMMENT '住宅/商业/综合'
) COMMENT='小区/楼盘';

CREATE TABLE properties (
    property_id INT PRIMARY KEY AUTO_INCREMENT,
    community_id INT NOT NULL,
    unit_number VARCHAR(30),
    area_sqm DECIMAL(8,2) NOT NULL COMMENT '面积(平方米)',
    bedrooms INT,
    floor_level INT,
    orientation VARCHAR(20) COMMENT '朝向',
    FOREIGN KEY (community_id) REFERENCES communities(community_id)
) COMMENT='房产单元';

CREATE TABLE property_transactions (
    txn_id INT PRIMARY KEY AUTO_INCREMENT,
    property_id INT NOT NULL,
    txn_type VARCHAR(10) COMMENT '买卖/租赁',
    txn_date DATE NOT NULL,
    price DECIMAL(14,2) NOT NULL COMMENT '成交价(万元)或月租(元)',
    buyer_name VARCHAR(100),
    seller_name VARCHAR(100),
    agent_name VARCHAR(100),
    FOREIGN KEY (property_id) REFERENCES properties(property_id)
) COMMENT='房产交易';

INSERT INTO communities VALUES
(1,'绿城翡翠城','西湖区','杭州',2015,2000,'住宅'),
(2,'滨江金色家园','滨江区','杭州',2018,1500,'住宅'),
(3,'万象城','上城区','杭州',2010,500,'商业'),
(4,'融创壹号院','余杭区','杭州',2020,800,'住宅'),
(5,'保利天悦','福田区','深圳',2019,1200,'住宅');

INSERT INTO properties VALUES
(1,1,'1-1-101',89.50,3,1,'南'),(2,1,'2-3-302',120.00,4,3,'南'),
(3,1,'5-2-1801',135.50,4,18,'东南'),(4,2,'A-1-501',95.00,3,5,'南'),
(5,2,'B-3-1202',110.00,3,12,'西南'),(6,2,'C-2-801',78.50,2,8,'东'),
(7,3,'A-101',200.00,0,1,'南'),(8,4,'1-1-301',88.00,3,3,'南'),
(9,4,'2-2-1501',125.00,4,15,'南'),(10,5,'T1-3201',180.00,4,32,'南'),
(11,5,'T2-1801',145.00,3,18,'东南'),(12,1,'3-1-602',105.00,3,6,'南');

INSERT INTO property_transactions VALUES
(1,1,'买卖','2024-01-10',350.00,'张三','王五','链家小李'),
(2,2,'买卖','2024-01-18',520.00,'李四','赵六','贝壳小张'),
(3,4,'买卖','2024-02-05',380.00,'钱七','孙八','链家小王'),
(4,6,'租赁','2024-01-15',4500.00,'周九',NULL,'自如小陈'),
(5,7,'买卖','2024-02-20',1200.00,'吴十','郑十一','链家小李'),
(6,8,'买卖','2024-03-01',320.00,'冯十二','何十三','贝壳小张'),
(7,10,'买卖','2024-01-25',1580.00,'秦十四','姜十五','链家小赵'),
(8,11,'租赁','2024-02-10',12000.00,'尤十六',NULL,'自如小陈'),
(9,3,'买卖','2024-03-10',680.00,'许十七','韩十八','链家小李'),
(10,12,'租赁','2024-03-01',5800.00,'朱十九',NULL,'贝壳小张'),
(11,9,'买卖','2024-02-28',510.00,'蒋二十','沈二一','链家小王'),
(12,5,'买卖','2024-03-15',420.00,'曹二二','彭二三','自如小陈');

-- ==================== Schema 5: logistics ====================
DROP TABLE IF EXISTS shipment_items;
DROP TABLE IF EXISTS shipments;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS logistics_customers;

CREATE TABLE warehouses (
    warehouse_id INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_name VARCHAR(100),
    city VARCHAR(50),
    capacity_tons DECIMAL(10,2),
    current_usage_pct DECIMAL(5,2) COMMENT '当前使用率%'
) COMMENT='仓库';

CREATE TABLE logistics_customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    company_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(50),
    city VARCHAR(50),
    contract_type VARCHAR(20) COMMENT '长期/临时',
    monthly_volume DECIMAL(10,2) COMMENT '月均发货量(吨)'
) COMMENT='物流客户';

CREATE TABLE shipments (
    shipment_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    ship_date DATE NOT NULL,
    destination_city VARCHAR(50),
    weight_kg DECIMAL(10,2) NOT NULL,
    shipping_cost DECIMAL(10,2) NOT NULL,
    delivery_date DATE,
    status VARCHAR(20) DEFAULT 'delivered' COMMENT 'pending/in_transit/delivered/returned',
    FOREIGN KEY (customer_id) REFERENCES logistics_customers(customer_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
) COMMENT='运单';

INSERT INTO warehouses VALUES
(1,'杭州仓','杭州',5000.00,72.50),(2,'上海仓','上海',8000.00,85.30),
(3,'广州仓','广州',6000.00,68.00),(4,'成都仓','成都',4000.00,55.20);

INSERT INTO logistics_customers VALUES
(1,'华为科技','张经理','深圳','长期',120.00),
(2,'阿里巴巴','李经理','杭州','长期',200.00),
(3,'小米科技','王经理','北京','长期',80.00),
(4,'字节跳动','赵经理','北京','临时',30.00),
(5,'美团','刘经理','上海','长期',150.00);

INSERT INTO shipments VALUES
(1,1,3,'2024-01-05','深圳',1500.00,4500.00,'2024-01-07','delivered'),
(2,2,1,'2024-01-08','杭州',800.00,1200.00,'2024-01-09','delivered'),
(3,2,1,'2024-01-15','上海',1200.00,2400.00,'2024-01-17','delivered'),
(4,3,2,'2024-01-20','北京',600.00,1800.00,'2024-01-23','delivered'),
(5,1,3,'2024-02-01','广州',2000.00,3000.00,'2024-02-03','delivered'),
(6,4,2,'2024-02-05','成都',350.00,1400.00,'2024-02-08','delivered'),
(7,5,2,'2024-02-10','上海',1800.00,2700.00,'2024-02-11','delivered'),
(8,2,1,'2024-02-15','深圳',950.00,2850.00,'2024-02-18','delivered'),
(9,3,4,'2024-02-20','成都',450.00,900.00,'2024-02-22','delivered'),
(10,1,3,'2024-03-01','北京',1100.00,4400.00,'2024-03-04','in_transit'),
(11,5,2,'2024-03-05','广州',2200.00,4400.00,'2024-03-07','delivered'),
(12,2,1,'2024-03-10','杭州',600.00,600.00,NULL,'pending'),
(13,4,2,'2024-03-12','上海',280.00,560.00,'2024-03-13','delivered'),
(14,3,2,'2024-03-15','北京',700.00,2100.00,NULL,'in_transit'),
(15,1,3,'2024-03-18','深圳',1600.00,4800.00,NULL,'pending');
