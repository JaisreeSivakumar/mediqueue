-- =============================================================================
-- MediQueue Big Data Pipeline - Apache Pig Cleaning & Transformation Script
-- Target: Clean raw operational timestamps, filter anomalies, and derive wait/service durations
-- =============================================================================

-- Register piggybank for additional date/time functions if needed
-- Configure job properties
SET default_parallel 2;
SET mapreduce.job.reduces 2;

-- 1. Load Raw Workflow Dataset from HDFS
raw_records = LOAD '/mediqueue/raw/raw_workflow_records.csv' USING PigStorage(',') AS (
    stage_id:chararray,
    visit_id:chararray,
    patient_id:chararray,
    age_group:chararray,
    gender:chararray,
    admission_type:chararray,
    primary_department_id:int,
    stage_sequence:int,
    stage_name:chararray,
    department_id:int,
    department_name:chararray,
    diagnostic_service_id:chararray,
    arrival_ts:chararray,
    service_start_ts:chararray,
    service_end_ts:chararray,
    visit_date:chararray,
    status:chararray
);

-- 2. Strip Header Row
no_header = FILTER raw_records BY stage_id != 'stage_id' AND stage_id != '';

-- 3. Deduplicate Records
unique_records = DISTINCT no_header;

-- 4. Filter Dirty Data (Null, Empty, or Incomplete Timestamps)
valid_timestamps = FILTER unique_records BY 
    (arrival_ts IS NOT NULL AND arrival_ts != '') AND
    (service_start_ts IS NOT NULL AND service_start_ts != '') AND
    (service_end_ts IS NOT NULL AND service_end_ts != '');

-- 5. Parse Dates and Calculate Durations (in Minutes)
-- Note: ToMilliSeconds(ToDate(ts, 'yyyy-MM-dd HH:mm:ss')) calculates Unix epoch milliseconds
parsed_durations = FOREACH valid_timestamps GENERATE
    stage_id,
    visit_id,
    patient_id,
    age_group,
    gender,
    admission_type,
    primary_department_id,
    stage_sequence,
    stage_name,
    department_id,
    department_name,
    diagnostic_service_id,
    arrival_ts,
    service_start_ts,
    service_end_ts,
    visit_date,
    -- Extract hour of arrival for congestion analytics
    GetHour(ToDate(arrival_ts, 'yyyy-MM-dd HH:mm:ss')) AS arrival_hour:int,
    -- Calculate queue wait time in minutes
    (long)(ToMilliSeconds(ToDate(service_start_ts, 'yyyy-MM-dd HH:mm:ss')) - ToMilliSeconds(ToDate(arrival_ts, 'yyyy-MM-dd HH:mm:ss'))) / 60000L AS wait_time_min:long,
    -- Calculate active service duration in minutes
    (long)(ToMilliSeconds(ToDate(service_end_ts, 'yyyy-MM-dd HH:mm:ss')) - ToMilliSeconds(ToDate(service_start_ts, 'yyyy-MM-dd HH:mm:ss'))) / 60000L AS service_dur_min:long;

-- 6. Filter Out Inverted Timestamps (Prevent Negative Durations)
cleaned_records = FILTER parsed_durations BY 
    (wait_time_min >= 0L AND wait_time_min <= 600L) AND 
    (service_dur_min >= 0L AND service_dur_min <= 480L);

-- 7. Add Total Stage Duration & Final Format Projection
final_dataset = FOREACH cleaned_records GENERATE
    stage_id,
    visit_id,
    patient_id,
    age_group,
    gender,
    admission_type,
    primary_department_id,
    stage_sequence,
    stage_name,
    department_id,
    department_name,
    diagnostic_service_id,
    arrival_ts,
    service_start_ts,
    service_end_ts,
    visit_date,
    arrival_hour,
    wait_time_min,
    service_dur_min,
    (wait_time_min + service_dur_min) AS total_stage_time_min:long;

-- 8. Store Cleaned Dataset into HDFS
RMF /mediqueue/cleaned/workflow_cleaned;
STORE final_dataset INTO '/mediqueue/cleaned/workflow_cleaned' USING PigStorage(',');
