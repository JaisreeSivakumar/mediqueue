#!/usr/bin/env bash
# =============================================================================
# MediQueue Master Pipeline Orchestrator
# Executes the entire Big Data pipeline end-to-end:
# 1. Verification of Docker Databases (MySQL 8.0, MongoDB 6.0) & HDFS
# 2. Stage 2: Java JDBC Extraction & Data Validation -> HDFS Raw
# 3. Stage 3: Apache Pig 0.18 MapReduce Data Cleaning -> HDFS Cleaned
# 4. Stage 4: Apache Hive Aggregations & Bottleneck Detection
# 5. Stage 5: MongoDB Document Loading & Indexing
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export HADOOP_HOME="/opt/hadoop-3.5.0"
export PIG_HOME="/mnt/c/pig-0.18.0"
export PATH="$PIG_HOME/bin:$HADOOP_HOME/bin:$JAVA_HOME/bin:$PATH"

echo "================================================================="
echo "   MEDIQUEUE HOSPITAL BIG DATA ANALYTICS PIPELINE               "
echo "   End-to-End Orchestrator: MySQL -> HDFS -> Pig -> Hive -> Mongo"
echo "================================================================="

# Step 0: Ensure Docker services are active
echo "[Step 0/5] Verifying database containers..."
docker start mediqueue-mysql mediqueue-mongo >/dev/null 2>&1 || true

# Step 1: Stage 2 - Java JDBC Extractor
echo -e "\n-----------------------------------------------------------------"
echo "[Step 1/4] Running Java JDBC Extractor & HDFS Raw Staging..."
bash "$SCRIPT_DIR/run_stage2.sh"

# Step 2: Stage 3 - Apache Pig Transformation
echo -e "\n-----------------------------------------------------------------"
echo "[Step 2/4] Executing Apache Pig Cleaning & Duration Calculation..."
bash "$SCRIPT_DIR/run_stage3_pig.sh"

# Step 3: Stage 4 & 5 - Hive Aggregations & MongoDB Loader
echo -e "\n-----------------------------------------------------------------"
echo "[Step 3/4] Processing Hive Analytical Aggregations & Loading MongoDB..."
python3 "$SCRIPT_DIR/loader/mongo_loader.py"

echo -e "\n================================================================="
echo "   ALL PIPELINE STAGES COMPLETED SUCCESSFULLY!                   "
echo "   Cleaned datasets stored in: hdfs://localhost:9000/mediqueue/  "
echo "   Analytics documents loaded in: mongodb://127.0.0.1:27017/     "
echo "================================================================="
