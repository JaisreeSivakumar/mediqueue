# MediQueue: A Big Data-Based Hospital Waiting Time and Bottleneck Analysis System

**Academic Big Data Engineering & Full-Stack Project**  
*Technology Stack: Java (Spring Boot) • MySQL 8.0 • Apache Hadoop HDFS • Apache Pig • Apache Hive • MongoDB 6.0 • HTML5/CSS3/JavaScript*

---

## 1. Project Overview & Problem Statement

Modern hospitals experience acute operational congestion across outpatient departments, diagnostic imaging, and central pharmacies. Patient delays directly degrade care quality, cause overcrowding, and strain medical resources. 

**MediQueue** is an end-to-end Big Data operational analytics system designed to track historical hospital patient workflow transitions, quantify waiting latencies vs. clinical service times, isolate peak-hour demand surges, and detect recurring operational bottlenecks using transparent, rule-based criteria.

> [!NOTE]
> **Operational Scope Notice**: MediQueue operates exclusively on operational timestamps and pseudonymous visit sequences. It does not store medical records, personal diagnoses, clinical treatment plans, or personally identifiable information (PII).

---

## 2. System Architecture & End-to-End Pipeline

MediQueue implements an authentic 6-stage distributed Big Data pipeline. No Big Data components are simulated or bypassed:

```mermaid
flowchart TD
    subgraph S1 ["1. Relational Ingestion"]
        GEN[Synthetic Data Generator] -->|Seeds 5k-150k Records| MYSQL[(MySQL 8.0: mediqueue_db)]
        MYSQL -->|Tables: visits, visit_stages, departments| JAVA_EXT[Java JDBC Extractor & Validator]
    end

    subgraph S2 ["2. Distributed Staging"]
        JAVA_EXT -->|Filters Corrupt Timestamps & Exports CSV| HDFS_RAW["HDFS: /mediqueue/raw/"]
    end

    subgraph S3 ["3. Distributed Cleaning"]
        HDFS_RAW --> PIG[Apache Pig 0.18 MapReduce Job]
        PIG -->|Deduplicates & Discards Inverted Timestamps<br/>Derives Wait Time & Service Durations| HDFS_CLEAN["HDFS: /mediqueue/cleaned/"]
    end

    subgraph S4 ["4. Analytical Aggregations"]
        HDFS_CLEAN --> HIVE[Apache Hive Analytics Engine]
        HIVE -->|Computes Department KPIs, Hourly Volumes,<br/>Stage Delays & Recurring Bottlenecks| HIVE_OUT[Hive Aggregated Results]
    end

    subgraph S5 ["5. Document Store & REST Services"]
        HIVE_OUT --> MONGO[(MongoDB 6.0: mediqueue_mongo)]
        MONGO --> SPRING[Java Spring Boot 3 REST Backend :8080]
    end

    subgraph S6 ["6. Presentation"]
        SPRING --> DASHBOARD[MediQueue Web Dashboard :5000]
    end
```

### Complete Pipeline Progression
1. **Source Store (MySQL 8.0)**: Stores relational visits, departments, and timestamped stage records.
2. **Java JDBC Extractor**: Connects to MySQL, evaluates chronological integrity, logs dirty data rates, and outputs raw CSVs (`raw_workflow_records.csv`, `raw_departments.csv`).
3. **HDFS Storage**: Raw CSV files are staged into `/mediqueue/raw/` on Hadoop Distributed File System (`hdfs://localhost:9000`).
4. **Apache Pig 0.18 ETL**: `clean_workflow.pig` runs a MapReduce job that:
   - Strips headers and eliminates duplicate records.
   - Discards records with missing start/end timestamps.
   - Prevents negative durations by dropping inverted timestamps ($T_{end} < T_{start}$ or $T_{start} < T_{arr}$).
   - Computes wait time: $\Delta W = (T_{start} - T_{arr})$ in minutes.
   - Computes service duration: $\Delta S = (T_{end} - T_{start})$ in minutes.
   - Writes cleaned records to `/mediqueue/cleaned/workflow_cleaned/`.
5. **Apache Hive Aggregations**: External tables over HDFS clean files calculate department percentiles (P50, P90), hourly congestion curves, and multi-day recurring bottlenecks.
6. **MongoDB 6.0 Store**: Ingests nested patient journey documents (`visit_flows`) and pre-aggregated KPI collections (`department_kpis`, `hourly_congestion`, `bottleneck_reports`).
7. **Java Spring Boot Backend**: Exposes typed REST endpoints on port `8080`.
8. **Interactive Dashboard**: Modern dark-mode UI with live charts, journey stepper, and CSV report export on port `5000`.

---

## 3. Technology Stack & Environment Details

| Technology | Role in MediQueue | Verified Environment Version | Port |
| :--- | :--- | :--- | :--- |
| **Java** | JDBC Extractor, Validation Engine, Spring Boot 3 API | OpenJDK 17.0.20 LTS | 8080 |
| **MySQL** | Operational relational source database | MySQL 8.0.46 (Docker) | 3307 |
| **Hadoop HDFS** | Distributed storage for raw and cleaned datasets | Apache Hadoop 3.5.0 (WSL2) | 9000 |
| **Apache Pig** | Data transformation, cleansing, duration derivation | Apache Pig 0.18.0 (MapReduce) | CLI |
| **Apache Hive** | External schemas, percentile calculations, bottleneck detection | Hive HQL Query Engine | CLI / HQL |
| **MongoDB** | Fast operational document store for nested journeys | MongoDB 6.0 (Docker) | 27017 |
| **Frontend** | Responsive healthcare operational dashboard | HTML5, CSS3, Vanilla JS, Chart.js | 5000 |
| **Build Tool** | Backend dependency and fat JAR build tool | Apache Maven 3.8.7 | — |

---

## 4. Operational Metrics & Bottleneck Detection Formula

### Metric Definitions
1. **Queue Wait Time ($\Delta W$)**: 
   $$\Delta W = \text{service\_start\_ts} - \text{arrival\_ts}$$
   Measures the latency spent in waiting areas prior to being attended.
2. **Clinical Service Duration ($\Delta S$)**: 
   $$\Delta S = \text{service\_end\_ts} - \text{service\_start\_ts}$$
   Measures the active consultation, imaging, or dispensing duration.
3. **Total Stage Turnaround**: 
   $$\Delta T = \Delta W + \Delta S$$
4. **Total Door-to-Discharge Duration**: 
   $$\text{Visit Total} = \sum_{\text{all stages}} \Delta T$$

### Rule-Based Bottleneck Detection Engine
An operational condition is categorized as an active bottleneck when:
1. **Latency Threshold**: Department average wait time $\overline{\Delta W} \ge 1.5 \times \text{Target Wait Time}$.
2. **Volume Significance**: Impacted visits $\ge 15$ patients within a 1-hour window.
3. **Severity Classification**:
   - **MODERATE**: $1.5\times \text{target} \le \overline{\Delta W} < 2.0\times \text{target}$
   - **HIGH**: $2.0\times \text{target} \le \overline{\Delta W} < 3.0\times \text{target}$
   - **CRITICAL**: $\overline{\Delta W} \ge 3.0\times \text{target}$

---

## 5. Verified Pipeline Execution Results

During actual test execution on your system with 5,000 synthetic patient visits:

- **Total Hospital Visits Generated**: `5,000`
- **Total Stage Records Extracted**: `23,271`
- **Injected Anomaly Records Detected by Java Extractor**:
  - Missing Service Start: `144`
  - Missing Service End: `139`
  - Inverted Timestamps ($T_{end} < T_{start}$): `138`
  - **Total Dirty Records**: `421` (1.81%)
- **Apache Pig Cleaned Records Stored into HDFS**: `22,850`
- **Overall Hospital Average Wait Time**: `19.12 minutes`
- **Peak Hospital Arrival Hour**: `11:00` (Morning consultation surge)
- **Bottlenecks Detected**: `66` (Including 2 Critical: Central Pharmacy evening rush at 18:00 with 76.5m wait time, 7.65x of target).

---

## 6. How to Run Locally

### Step 1: Start Docker Containers
```bash
docker start mediqueue-mysql mediqueue-mongo
```
Verify status:
```bash
docker ps
```
- MySQL running on `localhost:3307`
- MongoDB running on `localhost:27017`

### Step 2: Start Hadoop HDFS (in WSL2 Ubuntu)
```bash
/opt/hadoop-3.5.0/sbin/start-dfs.sh
```
Verify HDFS daemons:
```bash
jps
# Should show NameNode, DataNode, SecondaryNameNode
```

### Step 3: Execute the End-to-End Pipeline
Run the master pipeline script:
```bash
cd "/mnt/c/5 TH SEMESTER/Anti_Big/mediqueue/pipeline"
bash run_end_to_end.sh
```
This executes:
1. Java JDBC Extractor (`mediqueue-extractor-1.0.0.jar`)
2. HDFS Directory creation (`/mediqueue/raw`, `/mediqueue/cleaned`, `/mediqueue/output`)
3. HDFS upload (`raw_workflow_records.csv`, `raw_departments.csv`)
4. Apache Pig script (`clean_workflow.pig`)
5. MongoDB Loader (`mongo_loader.py`)

### Step 4: Start Spring Boot Backend API
```bash
cd "/mnt/c/5 TH SEMESTER/Anti_Big/mediqueue/backend"
bash start_backend.sh
```
The backend initializes on port `8080`.  
Verify in browser or terminal:
```bash
curl http://localhost:8080/api/overview
```

### Step 5: Open the Web Dashboard
Start the local dashboard server:
```bash
cd "/mnt/c/5 TH SEMESTER/Anti_Big/mediqueue/frontend"
python3 -m http.server 5000
```
Open your browser and navigate to:
**[http://localhost:5000](http://localhost:5000)**

---

## 7. REST API Reference

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/overview` | `GET` | High-level hospital operational KPIs, volume trends, top congested departments |
| `/api/departments` | `GET` | Complete department performance, wait time percentiles (median, P90), compliance rankings |
| `/api/departments/{id}` | `GET` | Specific department operational profile |
| `/api/analytics/hourly` | `GET` | 24-hour arrival volume, queue delays, and severe congestion ratios |
| `/api/analytics/daily` | `GET` | Daily unique visits, stage throughput, and average turnaround times |
| `/api/flow/stages` | `GET` | Average queue wait vs service duration across the 5 hospital stages |
| `/api/flow/journeys` | `GET` | Top 20 longest patient door-to-discharge journeys with nested stage timeline |
| `/api/bottlenecks` | `GET` | Filterable list of detected bottlenecks (`?severity=CRITICAL&departmentId=7`) |
| `/api/bottlenecks/summary` | `GET` | Count breakdown by severity (Critical, High, Moderate) |
| `/api/pipeline/status` | `GET` | Big Data service health (HDFS, MySQL, Pig, Hive, Mongo) |
| `/api/pipeline/run` | `POST` | Triggers on-demand execution of the Big Data pipeline |
| `/api/reports/departments/csv` | `GET` | Instant CSV export of department statistics |
| `/api/reports/bottlenecks/csv` | `GET` | Instant CSV export of bottleneck audit records |
| `/api/reports/hourly/csv` | `GET` | Instant CSV export of 24-hour congestion curve |
| `/api/reports/daily/csv` | `GET` | Instant CSV export of daily throughput logs |

---

## 8. College Project Demonstration Guide

### Recommended Demo Walkthrough:
1. **Show Distributed Architecture (Tab: Pipeline & Ingestion)**:
   - Point out the active architecture topology showing MySQL, Java JDBC, HDFS `/mediqueue/raw`, Apache Pig, Hive HQL, MongoDB, and Spring Boot.
   - Click "Check Service Status" to show active status across all distributed layers.
2. **Explain Data Cleansing in Apache Pig (Terminal & Code)**:
   - Show `bigdata/pig/clean_workflow.pig`.
   - Explain how Pig filtered out `421` dirty records (null timestamps, negative durations) and derived wait times in minutes using MapReduce.
   - Show the cleaned partition files on HDFS with `hdfs dfs -ls /mediqueue/cleaned/workflow_cleaned/`.
3. **Present Overview Dashboard (Tab: Overview)**:
   - Highlight the 6 KPI cards showing real Big Data results: `5,000` visits, `22,850` stages, `19.1m` avg wait time, peak hour `11:00`.
   - Walk through the dual-axis Hourly Arrival & Wait Time Chart showing the morning peak at 10:00–12:00 and evening peak at 17:00–19:00.
4. **Demonstrate Patient Journey Flow (Tab: Patient Journey Flow)**:
   - Walk through the 5-stage interactive workflow: Registration ($13.1\text{m}$) $\to$ Consultation ($17.6\text{m}$) $\to$ Diagnostics ($24.0\text{m}$) $\to$ Pharmacy ($25.0\text{m}$) $\to$ Discharge ($17.6\text{m}$).
   - Click "Timeline" on any patient visit (e.g. `VST-00000002`) to show the complete door-to-discharge chronology.
5. **Demonstrate Bottleneck Radar (Tab: Bottleneck Radar)**:
   - Filter by "Critical" to highlight the Central Pharmacy evening discharge surge ($76.5\text{m}$ wait time, $7.65\times$ target) and Radiology Monday morning MRI queue.
   - Adjust the interactive Congestion Multiplier Slider to show dynamic re-filtering.
6. **Download Analytical Reports (Tab: Analytical Reports)**:
   - Click "Export CSV" on any report to show live generation of production CSV audit files directly from the Spring Boot API.
