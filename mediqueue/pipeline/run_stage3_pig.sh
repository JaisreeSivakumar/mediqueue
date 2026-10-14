#!/usr/bin/env bash
# =============================================================================
# MediQueue Stage 3 Runner: Apache Pig Data Cleaning & Transformation
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PIG_SCRIPT="$PROJECT_ROOT/bigdata/pig/clean_workflow.pig"

export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export HADOOP_HOME="/opt/hadoop-3.5.0"
export PIG_HOME="/mnt/c/pig-0.18.0"
export PATH="$PIG_HOME/bin:$HADOOP_HOME/bin:$JAVA_HOME/bin:$PATH"

# Options for Java 17 compatibility with Hadoop 3 & Pig
export PIG_OPTS="-Djava.security.manager=allow -Dpig.use.overridden.hadoop.configs=true --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"

echo "================================================================="
echo "   STARTING STAGE 3: Apache Pig Cleaning & Transformation Pipeline"
echo "================================================================="
echo "[1/3] Verifying Pig script: $PIG_SCRIPT"
echo "[2/3] Executing Pig script on HDFS cluster..."

# Execute Pig
pig -f "$PIG_SCRIPT"

echo "[3/3] Inspecting Cleaned Output in HDFS..."
hdfs dfs -ls -h /mediqueue/cleaned/workflow_cleaned/
echo "--- Sample Cleaned Records (First 5 Rows) ---"
hdfs dfs -cat /mediqueue/cleaned/workflow_cleaned/part* | head -n 5

echo "================================================================="
echo "   STAGE 3 COMPLETED: Apache Pig Successfully Cleaned Datasets   "
echo "================================================================="
