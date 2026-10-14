#!/usr/bin/env bash
# =============================================================================
# MediQueue Stage 2 Runner: Extraction, Validation & HDFS Ingestion
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXTRACTOR_DIR="$SCRIPT_DIR/extractor"
SCRIPTS_DIR="$SCRIPT_DIR/scripts"

export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3307}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-mediqueue_db}"
export MYSQL_USER="${MYSQL_USER:-mediqueue}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-mediqueue_pass}"
export EXTRACTOR_OUTPUT_DIR="$EXTRACTOR_DIR/data/raw"

echo "================================================================="
echo "   STARTING STAGE 2: Java JDBC Extraction & HDFS Upload          "
echo "================================================================="

# Step 1: Execute Java JDBC Extractor
echo "[Step 1/3] Running Java JDBC Extractor & Record Validator..."
java -jar "$EXTRACTOR_DIR/target/mediqueue-extractor-1.0.0.jar"

# Step 2: Initialize HDFS Directories
echo "[Step 2/3] Setting up HDFS storage hierarchy..."
bash "$SCRIPTS_DIR/setup_hdfs.sh"

# Step 3: Upload Raw Datasets to HDFS
echo "[Step 3/3] Uploading raw datasets to HDFS /mediqueue/raw/..."
bash "$SCRIPTS_DIR/upload_to_hdfs.sh"

echo "================================================================="
echo "   STAGE 2 COMPLETED: Raw Records Successfully Ingested to HDFS  "
echo "================================================================="
