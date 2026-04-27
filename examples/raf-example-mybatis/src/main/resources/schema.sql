-- ========================================
-- RAF Framework MyBatis Example
-- Database Initialization Script
-- ========================================

-- Create database
CREATE DATABASE IF NOT EXISTS example_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE example_db;

-- ========================================
-- User Table
-- ========================================
DROP TABLE IF EXISTS t_user;

CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    email VARCHAR(100) COMMENT 'Email',
    age INT COMMENT 'Age',
    status INT DEFAULT 1 COMMENT 'Status (1: Active, 0: Inactive)',
    is_deleted INT DEFAULT 0 COMMENT 'Logic Delete (1: Deleted, 0: Not Deleted)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    create_by VARCHAR(50) COMMENT 'Creator',
    update_by VARCHAR(50) COMMENT 'Updater',
    version INT DEFAULT 0 COMMENT 'Version (Optimistic Lock)',
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';

-- ========================================
-- Sample Data
-- ========================================
INSERT INTO t_user (username, email, age, status, create_by, update_by) VALUES
('alice', 'alice@example.com', 25, 1, 'system', 'system'),
('bob', 'bob@example.com', 30, 1, 'system', 'system'),
('charlie', 'charlie@example.com', 28, 1, 'system', 'system'),
('david', 'david@example.com', 35, 1, 'system', 'system'),
('eve', 'eve@example.com', 22, 1, 'system', 'system'),
('frank', 'frank@example.com', 40, 0, 'system', 'system'),
('grace', 'grace@example.com', 27, 1, 'system', 'system'),
('henry', 'henry@example.com', 33, 1, 'system', 'system'),
('iris', 'iris@example.com', 29, 1, 'system', 'system'),
('jack', 'jack@example.com', 31, 1, 'system', 'system');

-- ========================================
-- Verify Data
-- ========================================
SELECT COUNT(*) AS total_users FROM t_user WHERE is_deleted = 0;
SELECT * FROM t_user WHERE is_deleted = 0 ORDER BY id;
