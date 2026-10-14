-- =============================================================================
-- MediQueue Big Data Pipeline - Rule-Based Bottleneck Detection Query
-- Flags recurring bottleneck points by department, stage, and recurring time windows
-- =============================================================================
USE mediqueue_hive;

INSERT OVERWRITE DIRECTORY 'hdfs://localhost:9000/mediqueue/output/bottleneck_analysis'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT 
    w.department_id,
    w.department_name,
    w.stage_name,
    w.arrival_hour,
    COUNT(w.stage_id) AS affected_visits,
    ROUND(AVG(w.wait_time_min), 2) AS avg_wait_time_min,
    MAX(w.wait_time_min) AS peak_wait_time_min,
    COALESCE(MAX(d.target_wait_min), 20) AS target_wait_min,
    ROUND(AVG(w.wait_time_min) / COALESCE(MAX(d.target_wait_min), 20), 2) AS congestion_multiplier,
    CASE 
        WHEN AVG(w.wait_time_min) >= 3.0 * COALESCE(MAX(d.target_wait_min), 20) THEN 'CRITICAL'
        WHEN AVG(w.wait_time_min) >= 2.0 * COALESCE(MAX(d.target_wait_min), 20) THEN 'HIGH'
        WHEN AVG(w.wait_time_min) >= 1.5 * COALESCE(MAX(d.target_wait_min), 20) THEN 'MODERATE'
        ELSE 'NORMAL'
    END AS severity_level,
    CONCAT(
        'Queue wait time averaged ', 
        ROUND(AVG(w.wait_time_min), 1), 
        ' min vs target of ', 
        COALESCE(MAX(d.target_wait_min), 20), 
        ' min across ', 
        COUNT(w.stage_id), 
        ' patient visits during hour ', 
        w.arrival_hour, 
        ':00.'
    ) AS bottleneck_rationale
FROM workflow_cleaned w
LEFT JOIN departments_ref d ON w.department_id = d.department_id
GROUP BY w.department_id, w.department_name, w.stage_name, w.arrival_hour
HAVING AVG(w.wait_time_min) >= 1.5 * COALESCE(MAX(d.target_wait_min), 20)
   AND COUNT(w.stage_id) >= 15
ORDER BY avg_wait_time_min DESC;
