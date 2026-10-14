#!/usr/bin/env bash
# =============================================================================
# MediQueue HDFS Directory Setup Script
# =============================================================================
set -e

HADOOP_BIN="/opt/hadoop-3.5.0/bin/hdfs"

if [ ! -f "$HADOOP_BIN" ]; then
    HADOOP_BIN="$(which hdfs 2>/dev/null || echo '')"
fi

if [ -z "$HADOOP_BIN" ]; then
    echo "[ERROR] HDFS binary not found. Please ensure Hadoop 3.5.0 is installed."
    exit 1
fi

echo "=== Initializing MediQueue HDFS Directory Structure ==="
echo "[1/3] Creating HDFS directories..."
$HADOOP_BIN dfs -mkdir -p /mediqueue/raw
$HADOOP_BIN dfs -mkdir -p /mediqueue/cleaned
$HADOOP_BIN dfs -mkdir -p /mediqueue/output

echo "[2/3] Setting directory permissions..."
$HADOOP_BIN dfs -chmod -R 777 /mediqueue

echo "[3/3] Verifying HDFS directories..."
$HADOOP_BIN dfs -ls /mediqueue

echo "=== HDFS Setup Completed Successfully ==="
