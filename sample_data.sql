-- ====================================================================
-- SharePlate: Sample Seed Data for MySQL 8.0+
-- Passwords below are hashed using secure salted SHA-256:
--   admin@shareplate.org    -> admin123
--   donor@greengarden.com   -> donor123
--   canteen@citycollege.edu -> donor123
--   relief@hopeshelter.org  -> ngo123
--   coordinator@care.org    -> ngo123
-- ====================================================================

USE shareplate_db;

-- 1. Insert Sample Users
INSERT IGNORE INTO users (user_id, name, email, password_hash, phone, role) VALUES
(1, 'System Administrator', 'admin@shareplate.org', 
 'c2hhcmVwbGF0ZXNhbHQz:esd9UUVLI4xuGV/GVdJEaqP56pvHQhWgHsu6WkLkQtg=', '+91 98765 00001', 'ADMIN'),

(2, 'Green Garden Bistro', 'donor@greengarden.com', 
 'c2hhcmVwbGF0ZXNhbHQx:rmcmZuF8lCSnOq7DKlS++/WBAs6WLWPW9aBAK4Bt1Eo=', '+91 98765 11111', 'DONOR'),

(3, 'City College Central Canteen', 'canteen@citycollege.edu', 
 'c2hhcmVwbGF0ZXNhbHQx:rmcmZuF8lCSnOq7DKlS++/WBAs6WLWPW9aBAK4Bt1Eo=', '+91 98765 22222', 'DONOR'),

(4, 'Sunrise Artisan Bakery', 'contact@sunrisebakery.com', 
 'c2hhcmVwbGF0ZXNhbHQx:rmcmZuF8lCSnOq7DKlS++/WBAs6WLWPW9aBAK4Bt1Eo=', '+91 98765 33333', 'DONOR'),

(5, 'Hope Shelter Foundation', 'relief@hopeshelter.org', 
 'c2hhcmVwbGF0ZXNhbHQy:VZm0VXHacRWEkQjT6ZNe5aa78aq1RtdCsnqFLps9iRo=', '+91 98765 44444', 'NGO'),

(6, 'Annapurna Care Network', 'coordinator@annapurnacare.org', 
 'c2hhcmVwbGF0ZXNhbHQy:VZm0VXHacRWEkQjT6ZNe5aa78aq1RtdCsnqFLps9iRo=', '+91 98765 55555', 'NGO');

-- 2. Insert Sample Food Donations
INSERT IGNORE INTO donations (donation_id, donor_id, food_name, category, quantity, preparation_datetime, safe_collection_deadline, pickup_location, storage_conditions, allergen_information, status) VALUES
(1, 2, 'Vegetable Biryani & Mixed Dal', 'Cooked Food', '45 meal portions (approx 18 kg)', 
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), 
 'Green Garden Bistro Kitchen Gate 2, Park Avenue', 'Hot Insulated Containers', 'Contains Dairy (ghee), no nuts', 'Available'),

(2, 3, 'Steamed Basmati Rice & Paneer Curry', 'Cooked Food', '60 meal boxes', 
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), 
 'Campus Dining Hall A, Loading Dock 1', 'Refrigerated Storage', 'Contains Dairy, Gluten-Free', 'Requested'),

(3, 4, 'Assorted Sourdough, Baguettes & Buns', 'Bakery & Bread', '35 loaves & 50 rolls', 
 DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 14 HOUR), 
 'Sunrise Bakery, Back Alley Dispatch', 'Room Temperature / Dry Box', 'Contains Gluten & Sesame Seeds', 'Approved'),

(4, 2, 'Fresh Farm Salad & Seasonal Fruits', 'Fruits & Vegetables', '25 kg fresh produce', 
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), 
 'Green Garden Bistro Loading Bay', 'Refrigerated Storage', 'None reported', 'Collected'),

(5, 3, 'Packed Fruit Juice & Mineral Water', 'Beverages', '120 tetra packs', 
 DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_ADD(NOW(), INTERVAL 72 HOUR), 
 'City College Pantry Storage, Ground Floor', 'Room Temperature', 'None reported', 'Delivered');

-- 3. Insert Sample Donation Requests
INSERT IGNORE INTO donation_requests (request_id, donation_id, ngo_id, request_date, request_status, beneficiary_count, request_notes) VALUES
(1, 2, 5, DATE_SUB(NOW(), INTERVAL 45 MINUTE), 'PENDING', 60, 'Hope Shelter evening meal distribution for 60 displaced workers.'),
(2, 3, 6, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'APPROVED', 85, 'Annapurna Care breakfast program for community shelter.'),
(3, 4, 5, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'COLLECTED', 50, 'Nutritious produce for evening shelter dining.'),
(4, 5, 6, DATE_SUB(NOW(), INTERVAL 20 HOUR), 'DELIVERED', 120, 'Unopened juice distribution to community children home.');
