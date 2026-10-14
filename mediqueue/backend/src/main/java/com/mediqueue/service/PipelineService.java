package com.mediqueue.service;

import com.mediqueue.model.PipelineMetadata;
import com.mediqueue.repository.PipelineMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class PipelineService {

    @Autowired
    private PipelineMetadataRepository metadataRepo;

    public Map<String, Object> getPipelineStatus() {
        Map<String, Object> status = new HashMap<>();

        PipelineMetadata meta = metadataRepo.findTopByOrderByLastRunTimestampDesc().orElse(null);
        status.put("metadata", meta);
        status.put("status", "ACTIVE");
        status.put("services", Map.of(
                "mysql", "ONLINE (Port 3307)",
                "hdfs", "ONLINE (hdfs://localhost:9000)",
                "pig", "READY (0.18.0 MapReduce)",
                "hive", "READY (HQL Analytics Engine)",
                "mongodb", "ONLINE (Port 27017)"
        ));
        status.put("hdfsPaths", Map.of(
                "rawDir", "/mediqueue/raw",
                "cleanedDir", "/mediqueue/cleaned/workflow_cleaned",
                "outputDir", "/mediqueue/output"
        ));
        return status;
    }

    public Map<String, Object> triggerPipelineExecution() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Run Stage 2 and Stage 3 pipeline script
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                    "bash /mnt/c/5\\ TH\\ SEMESTER/Anti_Big/mediqueue/pipeline/run_stage2.sh && " +
                    "bash /mnt/c/5\\ TH\\ SEMESTER/Anti_Big/mediqueue/pipeline/run_stage3_pig.sh && " +
                    "python3 /mnt/c/5\\ TH\\ SEMESTER/Anti_Big/mediqueue/pipeline/loader/mongo_loader.py");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder log = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append("\n");
                }
            }
            int exitCode = proc.waitFor();
            result.put("success", exitCode == 0);
            result.put("exitCode", exitCode);
            result.put("executionLog", log.toString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
