-- ============================================================
-- Spider 风格测试数据库 (5个独立Schema)
-- ============================================================

-- ==================== Schema 1: company_employee ====================
DROP TABLE IF EXISTS salary_payments;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    budget DECIMAL(12,2) DEFAULT 0,
    manager_id INT
) COMMENT='部门表';

CREATE TABLE employees (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    hire_date DATE NOT NULL,
    dept_id INT,
    position VARCHAR(50),
    salary DECIMAL(10,2),
    age INT,
    gender VARCHAR(10),
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) COMMENT='员工表';

CREATE TABLE salary_payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_id INT NOT NULL,
    pay_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    bonus DECIMAL(10,2) DEFAULT 0,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
) COMMENT='薪资发放表';

INSERT INTO departments VALUES
(1,'Engineering','Building A',500000,1),(2,'Marketing','Building B',300000,4),
(3,'Sales','Building C',250000,7),(4,'HR','Building A',150000,10),
(5,'Finance','Building D',200000,13),(6,'Research','Building E',400000,16);

INSERT INTO employees VALUES
(1,'John','Smith','john.s@corp.com','2020-03-15',1,'Senior Engineer',12000,35,'M'),
(2,'Jane','Doe','jane.d@corp.com','2021-06-01',1,'Engineer',9500,28,'F'),
(3,'Bob','Johnson','bob.j@corp.com','2019-01-10',1,'Tech Lead',15000,40,'M'),
(4,'Alice','Williams','alice.w@corp.com','2020-08-20',2,'Marketing Manager',11000,32,'F'),
(5,'Charlie','Brown','charlie.b@corp.com','2022-02-14',2,'Marketing Specialist',7500,26,'M'),
(6,'Diana','Davis','diana.d@corp.com','2021-11-30',2,'Content Writer',6500,24,'F'),
(7,'Edward','Wilson','edward.w@corp.com','2018-05-01',3,'Sales Director',14000,45,'M'),
(8,'Fiona','Taylor','fiona.t@corp.com','2020-09-15',3,'Account Manager',9000,30,'F'),
(9,'George','Anderson','george.a@corp.com','2022-07-01',3,'Sales Rep',6000,23,'M'),
(10,'Helen','Thomas','helen.t@corp.com','2019-04-01',4,'HR Manager',10500,38,'F'),
(11,'Ivan','Jackson','ivan.j@corp.com','2021-03-15',4,'Recruiter',7000,27,'M'),
(12,'Julia','White','julia.w@corp.com','2023-01-10',4,'HR Assistant',5500,22,'F'),
(13,'Kevin','Harris','kevin.h@corp.com','2017-06-01',5,'Finance Director',16000,48,'M'),
(14,'Laura','Martin','laura.m@corp.com','2020-10-01',5,'Accountant',8500,31,'F'),
(15,'Mike','Garcia','mike.g@corp.com','2022-04-01',5,'Financial Analyst',7800,25,'M'),
(16,'Nancy','Lee','nancy.l@corp.com','2018-02-01',6,'Research Director',17000,42,'F'),
(17,'Oscar','Robinson','oscar.r@corp.com','2021-08-15',6,'Researcher',9000,29,'M'),
(18,'Patricia','Clark','patricia.c@corp.com','2023-03-01',6,'Research Assistant',6200,24,'F'),
(19,'Quinn','Lewis','quinn.l@corp.com','2020-12-01',1,'Junior Engineer',7000,25,'M'),
(20,'Rachel','Walker','rachel.w@corp.com','2019-07-01',3,'Senior Sales Rep',10000,36,'F');

INSERT INTO salary_payments VALUES
(1,1,'2024-01-31',12000,2000),(2,1,'2024-02-29',12000,0),(3,2,'2024-01-31',9500,1000),
(4,2,'2024-02-29',9500,0),(5,3,'2024-01-31',15000,3000),(6,3,'2024-02-29',15000,0),
(7,4,'2024-01-31',11000,1500),(8,5,'2024-01-31',7500,500),(9,7,'2024-01-31',14000,4000),
(10,7,'2024-02-29',14000,0),(11,10,'2024-01-31',10500,1000),(12,13,'2024-01-31',16000,5000),
(13,16,'2024-01-31',17000,3000),(14,16,'2024-02-29',17000,0),(15,20,'2024-01-31',10000,2000);

-- ==================== Schema 2: university ====================
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS instructors;
DROP TABLE IF EXISTS students;

CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    age INT,
    gpa DECIMAL(3,2),
    major VARCHAR(50),
    enrollment_year INT
) COMMENT='学生表';

CREATE TABLE instructors (
    instructor_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50),
    title VARCHAR(50),
    hire_year INT
) COMMENT='教师表';

CREATE TABLE courses (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(100) NOT NULL,
    credits INT NOT NULL,
    department VARCHAR(50),
    instructor_id INT,
    max_enrollment INT DEFAULT 50,
    FOREIGN KEY (instructor_id) REFERENCES instructors(instructor_id)
) COMMENT='课程表';

CREATE TABLE enrollments (
    enrollment_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    grade DECIMAL(4,1),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
) COMMENT='选课表';

INSERT INTO students VALUES
(1,'Li Wei','M',20,3.8,'Computer Science',2022),(2,'Wang Fang','F',21,3.5,'Mathematics',2021),
(3,'Zhang Min','F',19,3.9,'Computer Science',2023),(4,'Chen Gang','M',22,3.2,'Physics',2020),
(5,'Liu Yan','F',20,3.7,'Mathematics',2022),(6,'Zhao Peng','M',21,2.8,'History',2021),
(7,'Sun Lei','M',23,3.1,'Physics',2020),(8,'Zhou Jing','F',19,3.6,'Computer Science',2023),
(9,'Wu Hao','M',20,3.4,'Engineering',2022),(10,'Xu Na','F',22,3.0,'History',2020),
(11,'Ma Jun','M',21,3.3,'Engineering',2021),(12,'Hu Ling','F',20,3.9,'Mathematics',2022),
(13,'Gao Fei','M',19,3.5,'Computer Science',2023),(14,'Lin Xue','F',21,2.9,'History',2021),
(15,'He Dong','M',22,3.7,'Engineering',2020);

INSERT INTO instructors VALUES
(1,'Prof. Wang','Computer Science','Professor',2010),(2,'Prof. Li','Mathematics','Professor',2008),
(3,'Dr. Zhang','Physics','Associate Professor',2015),(4,'Prof. Chen','History','Professor',2005),
(5,'Dr. Liu','Engineering','Associate Professor',2018),(6,'Dr. Zhao','Computer Science','Lecturer',2020);

INSERT INTO courses VALUES
(1,'Database Systems',3,'Computer Science',1,40),(2,'Linear Algebra',4,'Mathematics',2,60),
(3,'Quantum Physics',3,'Physics',3,30),(4,'World History',3,'History',4,50),
(5,'Data Structures',3,'Computer Science',1,45),(6,'Calculus',4,'Mathematics',2,55),
(7,'Mechanics',3,'Engineering',5,35),(8,'AI Fundamentals',3,'Computer Science',6,40),
(9,'Ancient History',3,'History',4,40),(10,'Thermodynamics',3,'Physics',3,30);

INSERT INTO enrollments VALUES
(1,1,1,'2024-Spring',92),(2,1,5,'2024-Spring',88),(3,1,8,'2024-Spring',95),
(4,2,2,'2024-Spring',85),(5,2,6,'2024-Spring',78),(6,3,1,'2024-Spring',96),
(7,3,5,'2024-Spring',91),(8,4,3,'2024-Spring',72),(9,4,10,'2024-Spring',68),
(10,5,2,'2024-Spring',90),(11,5,6,'2024-Spring',87),(12,6,4,'2024-Spring',65),
(13,6,9,'2024-Spring',70),(14,7,3,'2024-Spring',75),(15,7,10,'2024-Spring',73),
(16,8,1,'2024-Spring',89),(17,8,8,'2024-Spring',93),(18,9,7,'2024-Spring',82),
(19,10,4,'2024-Spring',60),(20,10,9,'2024-Spring',62),(21,11,7,'2024-Spring',80),
(22,12,2,'2024-Spring',95),(23,12,6,'2024-Spring',92),(24,13,5,'2024-Spring',86),
(25,13,8,'2024-Spring',90),(26,14,4,'2024-Spring',58),(27,14,9,'2024-Spring',63),
(28,15,7,'2024-Spring',85),(29,3,2,'2023-Fall',88),(30,1,2,'2023-Fall',84);

-- ==================== Schema 3: store ====================
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS categories;

CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL
) COMMENT='商品分类';

CREATE TABLE products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    category_id INT,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    brand VARCHAR(50),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
) COMMENT='商品表';

CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    city VARCHAR(50),
    registration_date DATE,
    vip_level INT DEFAULT 0
) COMMENT='客户表';

CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    order_date DATE NOT NULL,
    total_amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'pending',
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
) COMMENT='订单表';

CREATE TABLE order_items (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
) COMMENT='订单明细';

INSERT INTO categories VALUES (1,'Electronics'),(2,'Clothing'),(3,'Books'),(4,'Home'),(5,'Sports');

INSERT INTO products VALUES
(1,'Laptop',1,999.99,50,'Dell'),(2,'Smartphone',1,699.99,120,'Samsung'),
(3,'Headphones',1,149.99,200,'Sony'),(4,'T-Shirt',2,29.99,500,'Nike'),
(5,'Jeans',2,59.99,300,'Levi\'s'),(6,'Novel',3,15.99,1000,'Penguin'),
(7,'Textbook',3,45.99,200,'OReilly'),(8,'Desk Lamp',4,39.99,150,'Philips'),
(9,'Yoga Mat',5,25.99,180,'Adidas'),(10,'Running Shoes',5,119.99,100,'Nike'),
(11,'Tablet',1,449.99,80,'Apple'),(12,'Jacket',2,89.99,200,'Zara');

INSERT INTO customers VALUES
(1,'Tom Chen','tom@mail.com','Beijing','2023-01-15',2),
(2,'Lisa Wang','lisa@mail.com','Shanghai','2023-03-20',1),
(3,'Jack Liu','jack@mail.com','Guangzhou','2022-11-01',3),
(4,'Mary Zhang','mary@mail.com','Beijing','2024-01-10',0),
(5,'David Li','david@mail.com','Shenzhen','2023-06-15',1),
(6,'Sarah Wu','sarah@mail.com','Shanghai','2022-08-20',2),
(7,'Peter Zhao','peter@mail.com','Chengdu','2023-09-01',0),
(8,'Amy Sun','amy@mail.com','Hangzhou','2024-02-01',1);

INSERT INTO orders VALUES
(1,1,'2024-01-15',1149.98,'completed'),(2,1,'2024-02-20',29.99,'completed'),
(3,2,'2024-01-20',699.99,'completed'),(4,3,'2024-01-25',1509.97,'completed'),
(5,3,'2024-03-01',59.99,'shipped'),(6,4,'2024-02-14',15.99,'completed'),
(7,5,'2024-02-28',169.98,'completed'),(8,6,'2024-01-10',539.98,'completed'),
(9,6,'2024-03-05',89.99,'pending'),(10,7,'2024-03-10',25.99,'pending'),
(11,2,'2024-03-15',449.99,'shipped'),(12,8,'2024-03-01',119.99,'completed');

INSERT INTO order_items VALUES
(1,1,1,1,999.99),(2,1,3,1,149.99),(3,2,4,1,29.99),(4,3,2,1,699.99),
(5,4,1,1,999.99),(6,4,7,2,45.99),(7,4,8,1,39.99),(8,4,3,1,149.99),
(9,5,5,1,59.99),(10,6,6,1,15.99),(11,7,3,1,149.99),(12,7,9,1,25.99),
(13,8,11,1,449.99),(14,8,12,1,89.99),(15,9,12,1,89.99),(16,10,9,1,25.99),
(17,11,11,1,449.99),(18,12,10,1,119.99);

-- ==================== Schema 4: music ====================
DROP TABLE IF EXISTS playlist_tracks;
DROP TABLE IF EXISTS playlists;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS albums;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS genres;

CREATE TABLE genres (
    genre_id INT PRIMARY KEY AUTO_INCREMENT,
    genre_name VARCHAR(50) NOT NULL
) COMMENT='音乐流派';

CREATE TABLE artists (
    artist_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    formed_year INT
) COMMENT='艺术家';

CREATE TABLE albums (
    album_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    artist_id INT,
    release_year INT,
    genre_id INT,
    FOREIGN KEY (artist_id) REFERENCES artists(artist_id),
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id)
) COMMENT='专辑';

CREATE TABLE tracks (
    track_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    album_id INT,
    duration_seconds INT,
    track_number INT,
    FOREIGN KEY (album_id) REFERENCES albums(album_id)
) COMMENT='曲目';

INSERT INTO genres VALUES (1,'Rock'),(2,'Pop'),(3,'Jazz'),(4,'Classical'),(5,'Hip-Hop');

INSERT INTO artists VALUES
(1,'The Beatles','UK',1960),(2,'Taylor Swift','USA',2004),(3,'Miles Davis','USA',1944),
(4,'Beethoven','Germany',1770),(5,'Eminem','USA',1996),(6,'Coldplay','UK',1996),
(7,'Adele','UK',2006),(8,'Jay Chou','Taiwan',1997);

INSERT INTO albums VALUES
(1,'Abbey Road',1,1969,1),(2,'Let It Be',1,1970,1),(3,'1989',2,2014,2),
(4,'Folklore',2,2020,2),(5,'Kind of Blue',3,1959,3),(6,'Symphony No.9',4,1824,4),
(7,'The Eminem Show',5,2002,5),(8,'A Rush of Blood',6,2002,1),(9,'25',7,2015,2),
(10,'Fantasy',8,2001,2);

INSERT INTO tracks VALUES
(1,'Come Together',1,259,1),(2,'Something',1,182,2),(3,'Here Comes the Sun',1,185,3),
(4,'Let It Be',2,243,1),(5,'Shake It Off',3,219,1),(6,'Blank Space',3,231,2),
(7,'Cardigan',4,239,1),(8,'Exile',4,253,2),(9,'So What',5,561,1),
(10,'Blue in Green',5,327,2),(11,'Ode to Joy',6,390,1),(12,'Without Me',7,290,1),
(13,'Lose Yourself',7,326,2),(14,'The Scientist',8,309,1),(15,'Clocks',8,308,2),
(16,'Hello',9,295,1),(17,'Rolling in the Deep',9,228,2),(18,'Blue and White Porcelain',10,270,1);

-- ==================== Schema 5: hospital ====================
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS patients;

CREATE TABLE patients (
    patient_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    phone VARCHAR(20),
    blood_type VARCHAR(5),
    insurance_type VARCHAR(30)
) COMMENT='患者表';

CREATE TABLE doctors (
    doctor_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    specialty VARCHAR(50),
    department VARCHAR(50),
    years_experience INT,
    title VARCHAR(30)
) COMMENT='医生表';

CREATE TABLE appointments (
    appointment_id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    diagnosis VARCHAR(200),
    treatment_cost DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'completed',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
) COMMENT='就诊记录';

CREATE TABLE prescriptions (
    prescription_id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_id INT NOT NULL,
    medicine_name VARCHAR(100),
    dosage VARCHAR(50),
    duration_days INT,
    cost DECIMAL(8,2),
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
) COMMENT='处方表';

INSERT INTO patients VALUES
(1,'Wang Lei','M','1985-03-15','13800001111','A','医保'),
(2,'Li Na','F','1990-07-20','13800002222','B','医保'),
(3,'Zhang Wei','M','1978-11-05','13800003333','O','商保'),
(4,'Chen Mei','F','2000-01-30','13800004444','AB','医保'),
(5,'Liu Qiang','M','1965-09-12','13800005555','A','医保'),
(6,'Zhao Xia','F','1995-04-25','13800006666','B','自费'),
(7,'Sun Jian','M','1988-12-08','13800007777','O','商保'),
(8,'Zhou Yan','F','1972-06-18','13800008888','A','医保');

INSERT INTO doctors VALUES
(1,'Dr. Yang','Cardiology','Internal Medicine',20,'Chief'),
(2,'Dr. Huang','Orthopedics','Surgery',15,'Associate Chief'),
(3,'Dr. Wu','Pediatrics','Pediatrics',10,'Attending'),
(4,'Dr. Zheng','Dermatology','Dermatology',8,'Attending'),
(5,'Dr. Ma','Neurology','Internal Medicine',25,'Chief');

INSERT INTO appointments VALUES
(1,1,1,'2024-01-10','Hypertension',350.00,'completed'),
(2,2,2,'2024-01-15','Fracture',1200.00,'completed'),
(3,3,1,'2024-01-20','Arrhythmia',500.00,'completed'),
(4,4,3,'2024-02-01','Common Cold',120.00,'completed'),
(5,5,5,'2024-02-10','Migraine',400.00,'completed'),
(6,6,4,'2024-02-15','Eczema',200.00,'completed'),
(7,7,2,'2024-02-20','Sprain',800.00,'completed'),
(8,1,1,'2024-03-01','Hypertension Follow-up',280.00,'completed'),
(9,8,5,'2024-03-05','Insomnia',350.00,'completed'),
(10,2,4,'2024-03-10','Acne',150.00,'completed'),
(11,3,1,'2024-03-15','Arrhythmia Follow-up',450.00,'completed'),
(12,5,5,'2024-03-20','Migraine Follow-up',380.00,'completed');

INSERT INTO prescriptions VALUES
(1,1,'Amlodipine','5mg daily',30,85.00),(2,1,'Lisinopril','10mg daily',30,65.00),
(3,2,'Ibuprofen','400mg 3x daily',14,45.00),(4,3,'Metoprolol','50mg 2x daily',30,90.00),
(5,4,'Amoxicillin','500mg 3x daily',7,35.00),(6,5,'Sumatriptan','50mg as needed',10,120.00),
(7,6,'Hydrocortisone cream','Apply 2x daily',14,55.00),(8,7,'Diclofenac','50mg 2x daily',10,40.00),
(9,8,'Amlodipine','5mg daily',30,85.00),(10,9,'Zolpidem','10mg nightly',14,95.00),
(11,10,'Tretinoin cream','Apply nightly',30,75.00),(12,11,'Metoprolol','50mg 2x daily',30,90.00),
(13,12,'Sumatriptan','50mg as needed',10,120.00);
