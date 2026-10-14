package com.mediqueue.controller;

import com.mediqueue.model.StageFlow;
import com.mediqueue.model.VisitFlow;
import com.mediqueue.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flow")
public class FlowController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/stages")
    public ResponseEntity<List<StageFlow>> getStageFlows() {
        return ResponseEntity.ok(analyticsService.getStageFlows());
    }

    @GetMapping("/journeys")
    public ResponseEntity<List<VisitFlow>> getTopJourneys() {
        return ResponseEntity.ok(analyticsService.getTopLongestJourneys());
    }

    @GetMapping("/journeys/{visitId}")
    public ResponseEntity<VisitFlow> getJourneyByVisitId(@PathVariable String visitId) {
        return analyticsService.getJourney(visitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
