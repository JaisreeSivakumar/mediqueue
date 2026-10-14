package com.mediqueue.controller;

import com.mediqueue.model.BottleneckReport;
import com.mediqueue.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bottlenecks")
public class BottleneckController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<List<BottleneckReport>> getBottlenecks(
            @RequestParam(required = false, defaultValue = "ALL") String severity,
            @RequestParam(required = false) Integer departmentId) {
        return ResponseEntity.ok(analyticsService.getBottlenecks(severity, departmentId));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBottleneckSummary() {
        List<BottleneckReport> all = analyticsService.getBottlenecks("ALL", null);
        long critical = all.stream().filter(b -> "CRITICAL".equalsIgnoreCase(b.getSeverity())).count();
        long high = all.stream().filter(b -> "HIGH".equalsIgnoreCase(b.getSeverity())).count();
        long moderate = all.stream().filter(b -> "MODERATE".equalsIgnoreCase(b.getSeverity())).count();

        Map<String, Object> map = new HashMap<>();
        map.put("totalBottlenecks", all.size());
        map.put("criticalCount", critical);
        map.put("highCount", high);
        map.put("moderateCount", moderate);
        return ResponseEntity.ok(map);
    }
}
