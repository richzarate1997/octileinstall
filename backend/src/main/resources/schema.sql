-- Create database if not exists
CREATE DATABASE IF NOT EXISTS octileinstall;
USE octileinstall;

-- Users table (Alberto & admin staff)
CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role ENUM('ADMIN', 'VIEWER') DEFAULT 'VIEWER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Inquiries table (Customer project inquiries)
CREATE TABLE IF NOT EXISTS inquiries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(255) NOT NULL,
  customer_email VARCHAR(255) NOT NULL,
  customer_phone VARCHAR(20),
  project_type ENUM(
    'Custom Shower',
    'Bathroom Floors & Walls',
    'Kitchen Backsplashes',
    'Flooring - Tile, Marble & Mosaic',
    'Tub Surrounds',
    'Repairs'
  ),
  city VARCHAR(255),
  message TEXT,
  status ENUM('NEW', 'CONTACTED', 'QUOTED', 'SCHEDULED', 'COMPLETED', 'ARCHIVED') DEFAULT 'NEW',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by INT,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Inspections table (Scheduled site visits)
CREATE TABLE IF NOT EXISTS inspections (
  id INT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id INT,
  scheduled_date DATETIME NOT NULL,
  scheduled_time TIME,
  location VARCHAR(500),
  status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE CASCADE
);

-- Indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_inquiry_status ON inquiries(status);
CREATE INDEX IF NOT EXISTS idx_inquiry_created ON inquiries(created_at);
CREATE INDEX IF NOT EXISTS idx_inspection_scheduled ON inspections(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_inquiry_created_by ON inquiries(created_by);

-- Insert initial admin user (password: admin123, BCrypt hashed)
-- Hash generated: $2a$10$slYQmyNdGzin7olVN3nCm.prwvfqo0E5C6/Y0pHK5r7W5Z8X9Z9fW
INSERT INTO users (email, password_hash, full_name, role)
VALUES ('alberto@octileinstall.com', '$2a$10$slYQmyNdGzin7olVN3nCm.prwvfqo0E5C6/Y0pHK5r7W5Z8X9Z9fW', 'Alberto', 'ADMIN')
ON DUPLICATE KEY UPDATE password_hash=password_hash;
