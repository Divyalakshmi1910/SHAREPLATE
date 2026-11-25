🍽️ SharePlate – Food Donation & Redistribution System

A complete Java web application connecting food donors with NGOs to eliminate food waste and fight hunger.

👩‍💻 Team Members
Name	Register Number
Divya Lakshmi S B	250701177
Kaviya B	250701338
Gokila A	250701197
Dhanushri V	250701154

Institution: Rajalakshmi Engineering College, Thandalam (Autonomous)
Department: Computer Science and Engineering
Faculty In-charges: Bhavani M (OOP Lab) | Vinothini S (DBMS Lab)
Academic Year: 2025–2026

📌 About the Project

SharePlate is a 3-tier Java web application that connects:

🍱 Food Donors (restaurants, canteens, bakeries, individuals)
🏢 NGOs / Charities (shelters, food banks, community kitchens)
🛡️ Admin (manages users and monitors all activity)

Built using Java Servlets, JDBC, MySQL, Maven, HTML, CSS, JavaScript deployed on Apache Tomcat 10.1.

🏗️ 3-Tier Architecture
┌─────────────────────────────────────┐
│   TIER 1: PRESENTATION              │
│   HTML + CSS + JavaScript (Browser) │
└──────────────┬──────────────────────┘
               │ HTTP / JSON
┌──────────────▼──────────────────────┐
│   TIER 2: APPLICATION (Tomcat)      │
│   Servlets → DAO → Models           │
└──────────────┬──────────────────────┘
               │ JDBC / SQL
┌──────────────▼──────────────────────┐
│   TIER 3: DATABASE                  │
│   MySQL 8 – shareplate_db           │
└─────────────────────────────────────┘
🔷 OOP Concepts Applied
Concept	Implementation
Encapsulation	All model fields (User, Donor, NGO, Admin) are private with getters/setters
Inheritance	Donor, NGO, Admin all extend the parent User class
Polymorphism	User.create() factory method instantiates the right subclass based on role; getDashboardUrl(), canCreateDonation() overridden per role
Abstraction	RoleAccessible.java interface defines behavioral contract for all actors
🗄️ Database Schema (shareplate_db)

3 Tables:

users
Column	Type	Description
user_id	INT PK AUTO_INCREMENT	Unique user ID
name	VARCHAR(120)	Full name
email	VARCHAR(120) UNIQUE	Login email
password_hash	VARCHAR(255)	Salted SHA-256 hash
phone	VARCHAR(25)	Contact number
role	ENUM('DONOR','NGO','ADMIN')	User role
created_at	TIMESTAMP	Registration time
donations
Column	Type	Description
donation_id	INT PK AUTO_INCREMENT	Unique donation ID
donor_id	INT FK → users	Who donated
food_name	VARCHAR(150)	Name of food
quantity	VARCHAR(100)	Amount/portions
status	ENUM	Available/Requested/Collected
donation_requests
Column	Type	Description
request_id	INT PK AUTO_INCREMENT	Unique request ID
donation_id	INT FK → donations	Which food
ngo_id	INT FK → users	Which NGO requested
request_status	ENUM	PENDING/APPROVED/REJECTED
⚡ Test Accounts
Role	Email	Password
🛡️ Admin	admin@shareplate.org	admin123
🍱 Donor	donor@greengarden.com	donor123
🏢 NGO	relief@hopeshelter.org	ngo123
🚀 How to Run Locally
Prerequisites
Java JDK 17+
Apache Maven 3.6+
Apache Tomcat 10.1
MySQL 8.0
Step 1 – Set Up MySQL Database

Open MySQL and run:

sql
source schema.sql;
source sample_data.sql;

This creates shareplate_db with all tables and sample data.

Step 2 – Configure Database Password

Edit src/main/resources/db.properties:

properties
db.url=jdbc:mysql://localhost:3306/shareplate_db?useSSL=false&serverTimezone=UTC
db.username=root
db.password=YOUR_MYSQL_PASSWORD
Step 3 – Build with Maven
bash
mvn clean package

This generates target/shareplate.war

Step 4 – Deploy to Tomcat
cmd
copy target\shareplate.war C:\Tomcat\webapps\

Then start Tomcat:

cmd
set "CATALINA_HOME=C:\Tomcat"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
C:\Tomcat\bin\startup.bat
Step 5 – Open in Browser
http://localhost:8080/shareplate/
🔒 Security Features
✅ Password Hashing – SHA-256 with random 16-byte salt (never stored plain text)
✅ SQL Injection Prevention – All queries use JDBC PreparedStatement
✅ Session Authentication – HttpSession checked on every protected servlet
✅ Role-Based Access – Donors, NGOs, Admins see only their permitted pages
✅ Atomic Transactions – conn.setAutoCommit(false) prevents double-booking of donations
🗂️ Project Structure
shareplate/
├── pom.xml                         # Maven build config
├── schema.sql                      # Database creation script
├── sample_data.sql                 # Seed data script
└── src/main/
    ├── java/com/shareplate/
    │   ├── config/DatabaseConfig.java   # JDBC Connection
    │   ├── dao/                         # Data Access Layer
    │   │   ├── UserDao.java
    │   │   ├── DonationDao.java
    │   │   └── RequestDao.java
    │   ├── model/                       # OOP Entity Classes
    │   │   ├── User.java  (Parent)
    │   │   ├── Donor.java (Child)
    │   │   ├── NGO.java   (Child)
    │   │   └── Admin.java (Child)
    │   ├── servlet/                     # Controller Layer
    │   │   ├── AuthServlet.java
    │   │   ├── DonationServlet.java
    │   │   ├── RequestServlet.java
    │   │   └── AdminServlet.java
    │   └── util/
    │       ├── JsonUtil.java
    │       └── PasswordUtil.java
    └── webapp/
        ├── index.html
        ├── login.html
        ├── register.html
        ├── donor-dashboard.html
        ├── ngo-dashboard.html
        └── admin-dashboard.html
🛠️ Tech Stack
Layer	Technology
Frontend	HTML5, CSS3, JavaScript (Vanilla)
Backend	Java 17, Jakarta Servlets
Database	MySQL 8.0, JDBC
Build Tool	Apache Maven
Server	Apache Tomcat 10.1
Security	SHA-256 Password Hashing
📄 License

MIT License – © 2026 Divyalakshmi1910

Built with ❤️ to fight food waste and hunger.
