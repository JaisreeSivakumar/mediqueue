package com.mediqueue.controller;

import com.mediqueue.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(pipelineService.getPipelineStatus());
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> triggerPipeline() {
        return ResponseEntity.ok(pipelineService.triggerPipelineExecution());
    }
}
