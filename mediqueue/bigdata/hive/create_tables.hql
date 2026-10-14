-- =============================================================================
-- MediQueue Big Data Pipeline - Apache Hive External Schema Definition
-- Reads cleaned HDFS dataset produced by Apache Pig
-- =============================================================================

CREATE DATABASE IF NOT EXISTS mediqueue_hive;
USE mediqueue_hive;

-- Drop table if exists
DROP TABLE IF EXISTS workflow_cleaned;
DROP TABLE IF EXISTS departments_ref;

-- 1. External Table: Cleaned Hospital Workflow Stages
CREATE EXTERNAL TABLE IF NOT EXISTS workflow_cleaned (
    stage_id STRING,
    visit_id STRING,
    patient_id STRING,
    age_group STRING,
    gender STRING,
    admission_type STRING,
    primary_department_id INT,
    stage_sequence INT,
    stage_name STRING,
    department_id INT,
    department_name STRING,
    diagnostic_service_id STRING,
    arrival_ts STRING,
    service_start_ts STRING,
    service_end_ts STRING,
    visit_date STRING,
    arrival_hour INT,
    wait_time_min BIGINT,
    service_dur_min BIGINT,
    total_stage_time_min BIGINT
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION 'hdfs://localhost:9000/mediqueue/cleaned/workflow_cleaned';

-- 2. External Table: Departments Reference Data
CREATE EXTERNAL TABLE IF NOT EXISTS departments_ref (
    department_id INT,
    department_code STRING,
    department_name STRING,
    floor_number INT,
    target_wait_min INT
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION 'hdfs://localhost:9000/mediqueue/raw'
TBLPROPERTIES ("skip.header.line.count"="1");
