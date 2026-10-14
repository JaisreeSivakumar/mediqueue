#!/usr/bin/env bash
# =============================================================================
# MediQueue Raw Data HDFS Upload Script
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DATA_DIR="$PROJECT_ROOT/pipeline/extractor/data/raw"

HADOOP_BIN="/opt/hadoop-3.5.0/bin/hdfs"
if [ ! -f "$HADOOP_BIN" ]; then
    HADOOP_BIN="$(which hdfs 2>/dev/null || echo '')"
fi

if [ -z "$HADOOP_BIN" ]; then
    echo "[ERROR] HDFS command not found."
    exit 1
fi

echo "=== Uploading MediQueue Raw Datasets to HDFS ==="
echo "[1/3] Checking local extracted CSV files in: $DATA_DIR"

if [ ! -f "$DATA_DIR/raw_departments.csv" ] || [ ! -f "$DATA_DIR/raw_workflow_records.csv" ]; then
    echo "[ERROR] Extracted raw CSV files not found in $DATA_DIR."
    echo "Please execute the Java JdbcExtractor first."
    exit 1
fi

echo "[2/3] Uploading CSV files to /mediqueue/raw/ on HDFS..."
cd "$DATA_DIR"
$HADOOP_BIN dfs -put -f raw_departments.csv raw_workflow_records.csv /mediqueue/raw/

echo "[3/3] Verifying HDFS destination..."
$HADOOP_BIN dfs -ls -h /mediqueue/raw/

echo "=== HDFS Ingestion Completed Successfully ==="
