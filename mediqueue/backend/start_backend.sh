#!/usr/bin/env bash
# =============================================================================
# MediQueue Spring Boot Backend Startup Script
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/target/mediqueue-backend-1.0.0.jar"

export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PORT="${MYSQL_PORT:-3307}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-mediqueue_db}"
export MYSQL_USER="${MYSQL_USER:-mediqueue}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-mediqueue_pass}"
export MONGO_HOST="${MONGO_HOST:-127.0.0.1}"
export MONGO_PORT="${MONGO_PORT:-27017}"
export MONGO_DATABASE="${MONGO_DATABASE:-mediqueue_mongo}"

if [ ! -f "$JAR_FILE" ]; then
    echo "[INFO] Building MediQueue Backend Jar..."
    cd "$SCRIPT_DIR" && mvn clean package -DskipTests
fi

echo "================================================================="
echo "   STARTING MEDIQUEUE SPRING BOOT REST BACKEND (Port 8080)       "
echo "================================================================="
java -jar "$JAR_FILE"
