-- ============================================================
-- Stationery Management System - Database Initialization Script
-- ============================================================
-- This script is executed automatically by MySQL on first startup
-- via the docker-entrypoint-initdb.d mechanism.
-- It creates the three separate databases required by the
-- microservices architecture.
-- ============================================================

-- Database for Auth Service (users, roles, tokens)
CREATE DATABASE IF NOT EXISTS auth_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Database for Inventory Service (stationery items, categories, stock)
CREATE DATABASE IF NOT EXISTS inventory_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Database for Request Service (purchase/issue requests, approvals)
CREATE DATABASE IF NOT EXISTS request_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant privileges to root (already has them, but explicit for clarity)
GRANT ALL PRIVILEGES ON auth_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON request_db.* TO 'root'@'%';
FLUSH PRIVILEGES;
