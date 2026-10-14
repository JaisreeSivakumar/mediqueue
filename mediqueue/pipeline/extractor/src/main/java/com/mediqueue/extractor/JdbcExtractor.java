package com.mediqueue.extractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MediQueue JDBC Data Extractor and Operational Validator
 * 
 * Connects to MySQL, extracts hospital visit-stage workflow records,
 * performs validation for missing or inverted timestamps, logs dirty-data statistics,
 * and exports raw operational datasets for ingestion into Hadoop HDFS.
 */
public class JdbcExtractor {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final String outputDir;

    // Validation counters
    private int totalStagesExtracted = 0;
    private int totalValidRecords = 0;
    private int missingStartCount = 0;
    private int missingEndCount = 0;
    private int invertedTimestampCount = 0;
    private int validVisitsCount = 0;

    public JdbcExtractor() {
        this.host = getEnvOrDefault("MYSQL_HOST", "127.0.0.1");
        this.port = Integer.parseInt(getEnvOrDefault("MYSQL_PORT", "3307"));
        this.database = getEnvOrDefault("MYSQL_DATABASE", "mediqueue_db");
        this.user = getEnvOrDefault("MYSQL_USER", "mediqueue");
        this.password = getEnvOrDefault("MYSQL_PASSWORD", "mediqueue_pass");
        this.outputDir = getEnvOrDefault("EXTRACTOR_OUTPUT_DIR", "data/raw");
    }

    private static String getEnvOrDefault(String key, String def) {
        String val = System.getenv(key);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : def;
    }

    public Connection getConnection() throws SQLException {
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database);
        System.out.println("[INFO] Connecting to MySQL at: " + jdbcUrl + " as user '" + user + "'");
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    public void run() {
        System.out.println("=================================================================");
        System.out.println("     MediQueue Big Data Pipeline - Stage 2: JDBC Extractor       ");
        System.out.println("=================================================================");

        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            boolean created = outDir.mkdirs();
            if (created) {
                System.out.println("[INFO] Created output directory: " + outDir.getAbsolutePath());
            }
        }

        try (Connection conn = getConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL successfully.\n");

            // 1. Export Departments reference dataset
            exportDepartments(conn, new File(outDir, "raw_departments.csv"));

            // 2. Export and validate Visits & Stages operational workflow dataset
            exportWorkflowRecords(conn, new File(outDir, "raw_workflow_records.csv"));

            // 3. Print validation summary
            printSummaryReport();

        } catch (SQLException e) {
            System.err.println("[ERROR] Database connectivity or query failure: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("[ERROR] File I/O failure while writing CSV files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void exportDepartments(Connection conn, File outputFile) throws SQLException, IOException {
        System.out.println("[EXTRACT] Exporting departments table to: " + outputFile.getName());
        String query = "SELECT department_id, department_code, department_name, floor_number, target_wait_min FROM departments ORDER BY department_id";

        int count = 0;
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery();
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            // Header
            writer.write("department_id,department_code,department_name,floor_number,target_wait_min\n");

            while (rs.next()) {
                int id = rs.getInt("department_id");
                String code = sanitizeCsv(rs.getString("department_code"));
                String name = sanitizeCsv(rs.getString("department_name"));
                int floor = rs.getInt("floor_number");
                int targetWait = rs.getInt("target_wait_min");

                writer.write(String.format("%d,%s,%s,%d,%d\n", id, code, name, floor, targetWait));
                count++;
            }
        }
        System.out.println("[SUCCESS] Exported " + count + " departments.\n");
    }

    private void exportWorkflowRecords(Connection conn, File outputFile) throws SQLException, IOException {
        System.out.println("[EXTRACT] Exporting and validating hospital workflow records...");
        
        // Count total visits first
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM visits");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                validVisitsCount = rs.getInt(1);
            }
        }

        // Comprehensive operational join query
        String query = "SELECT " +
                "  s.stage_id, " +
                "  s.visit_id, " +
                "  v.patient_id, " +
                "  p.age_group, " +
                "  p.gender, " +
                "  v.admission_type, " +
                "  v.primary_department_id, " +
                "  s.stage_sequence, " +
                "  s.stage_name, " +
                "  s.department_id, " +
                "  d.department_name, " +
                "  s.diagnostic_service_id, " +
                "  s.arrival_ts, " +
                "  s.service_start_ts, " +
                "  s.service_end_ts, " +
                "  v.visit_date, " +
                "  v.status " +
                "FROM visit_stages s " +
                "JOIN visits v ON s.visit_id = v.visit_id " +
                "JOIN patients p ON v.patient_id = p.patient_id " +
                "JOIN departments d ON s.department_id = d.department_id " +
                "ORDER BY s.visit_id, s.stage_sequence";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery();
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            // Header for CSV
            writer.write("stage_id,visit_id,patient_id,age_group,gender,admission_type,primary_department_id," +
                    "stage_sequence,stage_name,department_id,department_name,diagnostic_service_id," +
                    "arrival_ts,service_start_ts,service_end_ts,visit_date,status\n");

            while (rs.next()) {
                totalStagesExtracted++;

                String stageId = rs.getString("stage_id");
                String visitId = rs.getString("visit_id");
                String patientId = rs.getString("patient_id");
                String ageGroup = rs.getString("age_group");
                String gender = rs.getString("gender");
                String admissionType = rs.getString("admission_type");
                int primaryDeptId = rs.getInt("primary_department_id");
                int stageSeq = rs.getInt("stage_sequence");
                String stageName = rs.getString("stage_name");
                int deptId = rs.getInt("department_id");
                String deptName = sanitizeCsv(rs.getString("department_name"));
                
                int diagServiceId = rs.getInt("diagnostic_service_id");
                String diagServiceStr = rs.wasNull() ? "0" : String.valueOf(diagServiceId);

                Timestamp arrTs = rs.getTimestamp("arrival_ts");
                Timestamp startTs = rs.getTimestamp("service_start_ts");
                Timestamp endTs = rs.getTimestamp("service_end_ts");
                String visitDate = rs.getDate("visit_date") != null ? rs.getDate("visit_date").toString() : "";
                String status = rs.getString("status");

                // Validation Checks
                boolean isClean = true;

                if (startTs == null) {
                    missingStartCount++;
                    isClean = false;
                }
                if (endTs == null) {
                    missingEndCount++;
                    isClean = false;
                }
                if (startTs != null && endTs != null) {
                    if (endTs.before(startTs) || (arrTs != null && startTs.before(arrTs))) {
                        invertedTimestampCount++;
                        isClean = false;
                    }
                }

                if (isClean) {
                    totalValidRecords++;
                }

                // Format timestamps (empty string if null)
                String arrStr = (arrTs != null) ? TS_FORMATTER.format(arrTs.toLocalDateTime()) : "";
                String startStr = (startTs != null) ? TS_FORMATTER.format(startTs.toLocalDateTime()) : "";
                String endStr = (endTs != null) ? TS_FORMATTER.format(endTs.toLocalDateTime()) : "";

                // Write row
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%d,%d,%s,%d,%s,%s,%s,%s,%s,%s,%s\n",
                        stageId, visitId, patientId, ageGroup, gender, admissionType, primaryDeptId,
                        stageSeq, stageName, deptId, deptName, diagServiceStr,
                        arrStr, startStr, endStr, visitDate, status));

                if (totalStagesExtracted % 5000 == 0) {
                    System.out.printf("  -> Extracted %d records (Clean: %d, Dirty: %d)...\n",
                            totalStagesExtracted, totalValidRecords, (totalStagesExtracted - totalValidRecords));
                }
            }
        }
    }

    private void printSummaryReport() {
        int dirtyRecordsCount = totalStagesExtracted - totalValidRecords;
        double dirtyRate = totalStagesExtracted > 0 ? ((double) dirtyRecordsCount / totalStagesExtracted) * 100.0 : 0.0;

        System.out.println("\n=================================================================");
        System.out.println("                 EXTRACTION & VALIDATION SUMMARY                 ");
        System.out.println("=================================================================");
        System.out.printf("Total Hospital Visits Exported   : %,d\n", validVisitsCount);
        System.out.printf("Total Workflow Stages Extracted   : %,d\n", totalStagesExtracted);
        System.out.printf("  ✓ Valid & Clean Records         : %,d (%.2f%%)\n", totalValidRecords, (100.0 - dirtyRate));
        System.out.printf("  ✗ Corrupt / Dirty Records Found : %,d (%.2f%%)\n", dirtyRecordsCount, dirtyRate);
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Detailed Breakdown of Injected Anomaly Records:");
        System.out.printf("  - Records Missing Service Start: %,d\n", missingStartCount);
        System.out.printf("  - Records Missing Service End  : %,d\n", missingEndCount);
        System.out.printf("  - Chronologically Inverted Times: %,d\n", invertedTimestampCount);
        System.out.println("-----------------------------------------------------------------");
        System.out.println("[INFO] Output CSV Files Generated:");
        System.out.printf("  1. %s/raw_departments.csv\n", outputDir);
        System.out.printf("  2. %s/raw_workflow_records.csv\n", outputDir);
        System.out.println("=================================================================\n");
    }

    private static String sanitizeCsv(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    public static void main(String[] args) {
        JdbcExtractor extractor = new JdbcExtractor();
        extractor.run();
    }
}
