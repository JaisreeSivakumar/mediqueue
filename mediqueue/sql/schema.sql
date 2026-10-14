-- =============================================================================
-- MediQueue Hospital Waiting Time & Bottleneck Analysis System
-- Schema Definition (MySQL 8.0+)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS mediqueue_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE mediqueue_db;

-- 1. Departments table
DROP TABLE IF EXISTS visit_stages;
DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS diagnostic_services;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL,
    floor_number INT NOT NULL DEFAULT 1,
    target_wait_min INT NOT NULL DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Diagnostic services reference
CREATE TABLE diagnostic_services (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_code VARCHAR(30) NOT NULL UNIQUE,
    service_name VARCHAR(150) NOT NULL,
    department_id INT NOT NULL,
    typical_duration_min INT NOT NULL DEFAULT 15,
    CONSTRAINT fk_diag_dept FOREIGN KEY (department_id) 
        REFERENCES departments(department_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Anonymized Patients (Operational only: No PII, age_group only)
CREATE TABLE patients (
    patient_id VARCHAR(64) PRIMARY KEY,
    age_group ENUM('0-17', '18-35', '36-50', '51-65', '65+') NOT NULL,
    gender ENUM('M', 'F', 'OTHER') NOT NULL,
    registered_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. Visits table
CREATE TABLE visits (
    visit_id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    visit_date DATE NOT NULL,
    admission_type ENUM('OPD', 'EMERGENCY', 'ROUTINE', 'FOLLOW_UP') NOT NULL DEFAULT 'OPD',
    primary_department_id INT NOT NULL,
    total_stages INT NOT NULL DEFAULT 1,
    status ENUM('COMPLETED', 'INCOMPLETE', 'CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_visit_date (visit_date),
    INDEX idx_visit_dept (primary_department_id),
    CONSTRAINT fk_visit_patient FOREIGN KEY (patient_id) 
        REFERENCES patients(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_primary_dept FOREIGN KEY (primary_department_id) 
        REFERENCES departments(department_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Visit Stages (Operational flow records)
-- stage_name enum values: REGISTRATION, CONSULTATION, DIAGNOSTIC, PHARMACY, DISCHARGE
CREATE TABLE visit_stages (
    stage_id VARCHAR(80) PRIMARY KEY,
    visit_id VARCHAR(64) NOT NULL,
    stage_sequence INT NOT NULL DEFAULT 1,
    stage_name ENUM('REGISTRATION', 'CONSULTATION', 'DIAGNOSTIC', 'PHARMACY', 'DISCHARGE') NOT NULL,
    department_id INT NOT NULL,
    diagnostic_service_id INT NULL,
    arrival_ts DATETIME NULL,
    service_start_ts DATETIME NULL,
    service_end_ts DATETIME NULL,
    INDEX idx_stages_visit (visit_id),
    INDEX idx_stages_dept (department_id),
    INDEX idx_stages_name (stage_name),
    INDEX idx_stages_arrival (arrival_ts),
    CONSTRAINT fk_stage_visit FOREIGN KEY (visit_id) 
        REFERENCES visits(visit_id) ON DELETE CASCADE,
    CONSTRAINT fk_stage_dept FOREIGN KEY (department_id) 
        REFERENCES departments(department_id) ON DELETE CASCADE,
    CONSTRAINT fk_stage_diag FOREIGN KEY (diagnostic_service_id) 
        REFERENCES diagnostic_services(service_id) ON DELETE SET NULL
) ENGINE=InnoDB;
