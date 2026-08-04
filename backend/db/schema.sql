-- Reference schema for the OctileInstall backend.
--
-- Hibernate owns the schema at runtime via spring.jpa.hibernate.ddl-auto=update. Keep this
-- in sync with the @Entity classes and use it to provision a database by hand, or as the
-- starting point for a real migration tool (Flyway/Liquibase) when this goes to production.
--
-- This lives in backend/db/ rather than src/main/resources/ deliberately. Spring Boot
-- auto-executes a classpath schema.sql against *embedded* datasources, so while it was in
-- resources/ the H2 test context tried to run this MySQL-specific script and died on the
-- CREATE DATABASE below. Off the classpath it is inert documentation, which is all it is
-- meant to be. spring.sql.init.mode=never in application.properties backs that up.
--
-- The status/project_type columns are VARCHAR rather than MySQL ENUM on purpose: the
-- entities use @Enumerated(EnumType.STRING), which persists the Java constant name
-- (CUSTOM_SHOWER), not the human-facing label ("Custom Shower"). Declaring a MySQL ENUM
-- of the display strings would reject every value the application actually writes.

CREATE DATABASE IF NOT EXISTS octileinstall;
USE octileinstall;

-- Users table (Alberto & admin staff)
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,  -- BCrypt, written by DataSeeder / AuthService
  full_name VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',   -- ADMIN | VIEWER
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

-- Inquiries table (Customer project inquiries)
CREATE TABLE IF NOT EXISTS inquiries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(255) NOT NULL,
  customer_email VARCHAR(255) NOT NULL,
  customer_phone VARCHAR(20),
  -- CUSTOM_SHOWER | BATHROOM_FLOORS_WALLS | KITCHEN_BACKSPLASHES
  -- | FLOORING | TUB_SURROUNDS | REPAIRS
  project_type VARCHAR(50),
  city VARCHAR(255),
  message TEXT,
  -- NEW | CONTACTED | QUOTED | SCHEDULED | COMPLETED | ARCHIVED
  status VARCHAR(50) NOT NULL DEFAULT 'NEW',
  notes TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  created_by BIGINT,
  CONSTRAINT fk_inquiry_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Inspections table (Scheduled site visits)
CREATE TABLE IF NOT EXISTS inspections (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id BIGINT,
  scheduled_date DATETIME(6) NOT NULL,
  scheduled_time TIME,
  location VARCHAR(500),
  -- SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED
  status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
  notes TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_inspection_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE CASCADE
);

-- Indexes for faster queries
CREATE INDEX idx_inquiry_status ON inquiries(status);
CREATE INDEX idx_inquiry_created ON inquiries(created_at);
CREATE INDEX idx_inquiry_created_by ON inquiries(created_by);
CREATE INDEX idx_inspection_scheduled ON inspections(scheduled_date);

-- No seed user here on purpose.
--
-- An earlier draft of this file INSERTed a hard-coded BCrypt hash. That hash was not a
-- real digest of any known password, so nobody could have logged in with it. Account
-- creation now lives in DataSeeder, which hashes with the same PasswordEncoder bean that
-- login verifies against.
