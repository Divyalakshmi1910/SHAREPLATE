# SharePlate – Food Donation & Redistribution System

**SharePlate** is a complete, modern, responsive web application connecting food donors (restaurants, college canteens, bakeries, caterers, and individuals) with registered NGOs, shelters, and community food banks to rescue surplus food and distribute it to people in need.

---

## 🌟 Architecture & OOP Implementation

SharePlate strictly adheres to all core Object-Oriented Programming (OOP) principles:

1. **Encapsulation**:
   * All entity model fields are declared `private` (`User`, `Donor`, `NGO`, `Admin`, `Donation`, `DonationRequest`).
   * Explicit, clean getters and setters control access and validation.
2. **Inheritance**:
   * Parent class: [`User.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/model/User.java)
   * Child classes: [`Donor.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/model/Donor.java), [`NGO.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/model/NGO.java), [`Admin.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/model/Admin.java) extending `User`.
3. **Polymorphism**:
   * Subclasses override role-specific methods from the parent class:
     * `getDashboardUrl()`: returns `"donor-dashboard.html"`, `"ngo-dashboard.html"`, or `"admin-dashboard.html"`.
     * `canCreateDonation()`: `Donor` (`true`), `NGO` (`false`).
     * `canRequestDonation()`: `Donor` (`false`), `NGO` (`true`).
     * `getRoleDisplayName()`: returns customized role description.
   * `User.create(...)` acts as a polymorphic factory instantiating the concrete child type.
4. **Abstraction**:
   * [`RoleAccessible.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/model/RoleAccessible.java): An interface establishing the behavioral contract for all system actors.

---

## 🗄️ Database Schema (`shareplate_db`)

The relational database consists of 3 connected tables:

### 1. `users`
* `user_id` (INT, Primary Key, Auto Increment)
* `name` (VARCHAR(120), NOT NULL)
* `email` (VARCHAR(120), NOT NULL, UNIQUE)
* `password_hash` (VARCHAR(255), NOT NULL) — Salted SHA-256
* `phone` (VARCHAR(25), NOT NULL)
* `role` (ENUM('DONOR', 'NGO', 'ADMIN'), NOT NULL)
* `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 2. `donations`
* `donation_id` (INT, Primary Key, Auto Increment)
* `donor_id` (INT, Foreign Key referencing `users(user_id)`)
* `food_name` (VARCHAR(150), NOT NULL)
* `category` (VARCHAR(60), NOT NULL)
* `quantity` (VARCHAR(100), NOT NULL)
* `preparation_datetime` (DATETIME, NOT NULL)
* `safe_collection_deadline` (DATETIME, NOT NULL)
* `pickup_location` (TEXT, NOT NULL)
* `storage_conditions` (VARCHAR(100), NOT NULL)
* `allergen_information` (VARCHAR(255), DEFAULT 'None reported')
* `status` (ENUM('Available', 'Requested', 'Approved', 'Collected', 'Delivered'))
* `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 3. `donation_requests`
* `request_id` (INT, Primary Key, Auto Increment)
* `donation_id` (INT, Foreign Key referencing `donations(donation_id)`)
* `ngo_id` (INT, Foreign Key referencing `users(user_id)`)
* `request_date` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
* `request_status` (ENUM('PENDING', 'APPROVED', 'REJECTED', 'COLLECTED', 'DELIVERED'))
* `beneficiary_count` (INT, DEFAULT 10)
* `request_notes` (TEXT)

---

## 🔒 Security Features

1. **Password Hashing**: Uses cryptographically secure random 16-byte salts and SHA-256 via [`PasswordUtil.java`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/src/main/java/com/shareplate/util/PasswordUtil.java). No plain-text passwords are ever stored.
2. **SQL Injection Prevention**: All queries in `UserDao`, `DonationDao`, and `RequestDao` strictly use JDBC `PreparedStatement` with parameterized placeholders (`?`).
3. **Database Transactions (`conn.setAutoCommit(false)`)**:
   * Atomically locks and updates food status to `Requested` when an NGO claims a batch, preventing concurrent double-claims.
   * Atomically transitions requests to `APPROVED` and donation to `Approved`.
   * Automatically resets donation status to `Available` when a request is `REJECTED`.
4. **Ownership Verification**:
   * Donors can only edit and delete their own donations.
   * Donors can only approve/reject requests for their own food listings.
5. **Role-Based Access Control**:
   * Servlets verify the authenticated `HttpSession` and reject unauthorized roles with `401 Unauthorized` or `403 Forbidden`.
6. **Environment Variable Configuration**:
   * MySQL credentials can be supplied via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`) or configured in `src/main/resources/db.properties`.

---

## ⚡ Seed Accounts for Immediate Testing

Run [`schema.sql`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/schema.sql) followed by [`sample_data.sql`](file:///C:/Users/arunb/.gemini/antigravity/scratch/shareplate/sample_data.sql) in MySQL. The seed accounts are:

| Role | Name | Email | Password |
|---|---|---|---|
| **Donor** | Green Garden Bistro | `donor@greengarden.com` | `donor123` |
| **Donor** | City College Central Canteen | `canteen@citycollege.edu` | `donor123` |
| **NGO** | Hope Shelter Foundation | `relief@hopeshelter.org` | `ngo123` |
| **NGO** | Annapurna Care Network | `coordinator@care.org` | `ngo123` |
| **Admin** | System Administrator | `admin@shareplate.org` | `admin123` |

---

## 🚀 How to Run the Application Locally

### Step 1: Set Up MySQL 8
1. Open your MySQL client (Command Line, Workbench, or phpMyAdmin):
   ```sql
   source C:/Users/arunb/.gemini/antigravity/scratch/shareplate/schema.sql;
   source C:/Users/arunb/.gemini/antigravity/scratch/shareplate/sample_data.sql;
   ```
2. This creates `shareplate_db`, creates all relational tables, and inserts sample seed data with salted password hashes.

### Step 2: Configure Database Credentials
Edit `src/main/resources/db.properties` with your local MySQL password:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/shareplate_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=your_mysql_password
```
*(Alternatively, set the environment variable `DB_PASSWORD=your_mysql_password`).*

### Step 3: Package with Maven
In the `shareplate` root directory, execute:
```bash
mvn clean package
```
This builds `target/shareplate.war`.

### Step 4: Deploy to Apache Tomcat 10.1
1. Copy `target/shareplate.war` into Tomcat's `webapps/` folder:
   ```cmd
   copy target\shareplate.war C:\apache-tomcat-10.1.x\webapps\
   ```
2. Start Tomcat (`bin\startup.bat`).
3. Open your browser to:
   ```
   http://localhost:8080/shareplate/
   ```
