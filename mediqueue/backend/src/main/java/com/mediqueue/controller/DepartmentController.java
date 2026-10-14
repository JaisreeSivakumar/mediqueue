package com.mediqueue.controller;

import com.mediqueue.model.DepartmentKpi;
import com.mediqueue.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<List<DepartmentKpi>> getAllDepartments() {
        return ResponseEntity.ok(analyticsService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentKpi> getDepartmentById(@PathVariable int id) {
        return analyticsService.getDepartment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
