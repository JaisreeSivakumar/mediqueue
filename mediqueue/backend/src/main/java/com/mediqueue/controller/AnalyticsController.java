package com.mediqueue.controller;

import com.mediqueue.model.DailySummary;
import com.mediqueue.model.HourlyCongestion;
import com.mediqueue.model.OverviewResponse;
import com.mediqueue.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @GetMapping("/analytics/hourly")
    public ResponseEntity<List<HourlyCongestion>> getHourlyCongestion() {
        return ResponseEntity.ok(analyticsService.getHourlyCongestion());
    }

    @GetMapping("/analytics/daily")
    public ResponseEntity<List<DailySummary>> getDailyTrends() {
        return ResponseEntity.ok(analyticsService.getDailySummaries());
    }
}
