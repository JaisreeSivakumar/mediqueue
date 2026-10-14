package com.mediqueue.controller;

import com.mediqueue.service.ReportExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping("/departments/csv")
    public ResponseEntity<InputStreamResource> downloadDepartmentsCsv() {
        ByteArrayInputStream in = reportExportService.exportDepartmentsCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=mediqueue_department_kpis.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/bottlenecks/csv")
    public ResponseEntity<InputStreamResource> downloadBottlenecksCsv() {
        ByteArrayInputStream in = reportExportService.exportBottlenecksCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=mediqueue_bottleneck_audit.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/hourly/csv")
    public ResponseEntity<InputStreamResource> downloadHourlyCsv() {
        ByteArrayInputStream in = reportExportService.exportHourlyCongestionCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=mediqueue_hourly_congestion.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/daily/csv")
    public ResponseEntity<InputStreamResource> downloadDailyCsv() {
        ByteArrayInputStream in = reportExportService.exportDailyTrendsCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=mediqueue_daily_summary.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }
}
