-- =============================================================================
-- MediQueue Big Data Pipeline - Apache Hive Analytical Queries
-- =============================================================================
USE mediqueue_hive;

-- -----------------------------------------------------------------------------
-- Query 1: Department-wise Waiting Time and Service Duration Metrics
-- -----------------------------------------------------------------------------
INSERT OVERWRITE DIRECTORY 'hdfs://localhost:9000/mediqueue/output/department_analytics'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT 
    w.department_id,
    w.department_name,
    COUNT(w.stage_id) AS total_visits_served,
    ROUND(AVG(w.wait_time_min), 2) AS avg_wait_time_min,
    MIN(w.wait_time_min) AS min_wait_time_min,
    MAX(w.wait_time_min) AS max_wait_time_min,
    ROUND(PERCENTILE_APPROX(w.wait_time_min, 0.50), 2) AS median_wait_time_min,
    ROUND(PERCENTILE_APPROX(w.wait_time_min, 0.90), 2) AS p90_wait_time_min,
    ROUND(AVG(w.service_dur_min), 2) AS avg_service_dur_min,
    ROUND(AVG(w.total_stage_time_min), 2) AS avg_stage_total_min,
    MAX(d.target_wait_min) AS target_wait_min,
    ROUND(100.0 * SUM(CASE WHEN w.wait_time_min > COALESCE(d.target_wait_min, 20) THEN 1 ELSE 0 END) / COUNT(w.stage_id), 2) AS pct_exceeding_target
FROM workflow_cleaned w
LEFT JOIN departments_ref d ON w.department_id = d.department_id
GROUP BY w.department_id, w.department_name
ORDER BY avg_wait_time_min DESC;

-- -----------------------------------------------------------------------------
-- Query 2: Hourly Patient Congestion and Peak Hour Analysis
-- -----------------------------------------------------------------------------
INSERT OVERWRITE DIRECTORY 'hdfs://localhost:9000/mediqueue/output/hourly_congestion'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT 
    arrival_hour,
    COUNT(stage_id) AS total_patients_arrived,
    ROUND(AVG(wait_time_min), 2) AS avg_hourly_wait_time,
    ROUND(AVG(service_dur_min), 2) AS avg_hourly_service_dur,
    ROUND(100.0 * SUM(CASE WHEN wait_time_min > 30 THEN 1 ELSE 0 END) / COUNT(stage_id), 2) AS pct_congested
FROM workflow_cleaned
GROUP BY arrival_hour
ORDER BY arrival_hour ASC;

-- -----------------------------------------------------------------------------
-- Query 3: Patient Journey Stage-Wise Flow Delays
-- Registration -> Consultation -> Diagnostics -> Pharmacy -> Discharge
-- -----------------------------------------------------------------------------
INSERT OVERWRITE DIRECTORY 'hdfs://localhost:9000/mediqueue/output/stage_flow_delays'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT 
    stage_sequence,
    stage_name,
    COUNT(DISTINCT visit_id) AS total_patients_processed,
    ROUND(AVG(wait_time_min), 2) AS avg_queue_wait_min,
    ROUND(AVG(service_dur_min), 2) AS avg_active_service_min,
    ROUND(AVG(total_stage_time_min), 2) AS avg_stage_duration_min
FROM workflow_cleaned
GROUP BY stage_sequence, stage_name
ORDER BY stage_sequence ASC;

-- -----------------------------------------------------------------------------
-- Query 4: Daily Volume and Turnaround Trends
-- -----------------------------------------------------------------------------
INSERT OVERWRITE DIRECTORY 'hdfs://localhost:9000/mediqueue/output/daily_trends'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT 
    visit_date,
    COUNT(DISTINCT visit_id) AS daily_unique_visits,
    COUNT(stage_id) AS daily_stages_completed,
    ROUND(AVG(wait_time_min), 2) AS daily_avg_wait_min,
    ROUND(AVG(service_dur_min), 2) AS daily_avg_service_min
FROM workflow_cleaned
GROUP BY visit_date
ORDER BY visit_date ASC;
