package com.mediqueue.service;

import com.mediqueue.model.*;
import com.mediqueue.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private DepartmentKpiRepository departmentRepo;

    @Autowired
    private HourlyCongestionRepository hourlyRepo;

    @Autowired
    private StageFlowRepository stageRepo;

    @Autowired
    private BottleneckReportRepository bottleneckRepo;

    @Autowired
    private DailySummaryRepository dailyRepo;

    @Autowired
    private VisitFlowRepository visitFlowRepo;

    @Autowired
    private PipelineMetadataRepository metadataRepo;

    public OverviewResponse getOverview() {
        OverviewResponse res = new OverviewResponse();

        PipelineMetadata meta = metadataRepo.findTopByOrderByLastRunTimestampDesc()
                .orElse(createDefaultMetadata());
        res.setPipelineInfo(meta);

        res.setTotalVisits(meta.getTotalVisitsAnalyzed());
        res.setTotalStages(meta.getTotalStagesCleaned());
        res.setTotalDepartments(meta.getTotalDepartments());
        res.setAvgWaitTimeMin(meta.getOverallAvgWaitMin());
        res.setAvgServiceDurationMin(meta.getOverallAvgServiceMin());
        res.setAvgTotalVisitDurationMin(meta.getOverallAvgVisitDurationMin());
        res.setBottlenecksCount(meta.getBottlenecksDetected());
        res.setPeakCongestionPeriod(meta.getPeakHour());

        List<DepartmentKpi> depts = departmentRepo.findAllByOrderByAvgWaitTimeMinDesc();
        res.setTopCongestedDepartments(depts.stream().limit(5).collect(Collectors.toList()));

        res.setHourlyTrends(hourlyRepo.findAllByOrderByArrivalHourAsc());
        res.setFlowSummary(stageRepo.findAllByOrderBySequenceAsc());

        return res;
    }

    public List<DepartmentKpi> getAllDepartments() {
        return departmentRepo.findAllByOrderByAvgWaitTimeMinDesc();
    }

    public Optional<DepartmentKpi> getDepartment(int id) {
        return departmentRepo.findByDepartmentId(id);
    }

    public List<HourlyCongestion> getHourlyCongestion() {
        return hourlyRepo.findAllByOrderByArrivalHourAsc();
    }

    public List<StageFlow> getStageFlows() {
        return stageRepo.findAllByOrderBySequenceAsc();
    }

    public List<BottleneckReport> getBottlenecks(String severity, Integer deptId) {
        List<BottleneckReport> list = bottleneckRepo.findAllByOrderByAvgWaitTimeMinDesc();
        return list.stream()
                .filter(b -> severity == null || severity.equalsIgnoreCase("ALL") || b.getSeverity().equalsIgnoreCase(severity))
                .filter(b -> deptId == null || b.getDepartmentId() == deptId)
                .collect(Collectors.toList());
    }

    public List<DailySummary> getDailySummaries() {
        return dailyRepo.findAllByOrderByVisitDateAsc();
    }

    public List<VisitFlow> getTopLongestJourneys() {
        return visitFlowRepo.findTop20ByOrderByTotalJourneyDurationMinDesc();
    }

    public Optional<VisitFlow> getJourney(String visitId) {
        return visitFlowRepo.findByVisitId(visitId);
    }

    private PipelineMetadata createDefaultMetadata() {
        PipelineMetadata m = new PipelineMetadata();
        m.setPipelineVersion("1.0.0");
        m.setTotalVisitsAnalyzed(5000);
        m.setTotalStagesCleaned(22850);
        m.setTotalDepartments(9);
        m.setBottlenecksDetected(66);
        m.setPeakHour("11:00");
        m.setOverallAvgWaitMin(19.12);
        m.setOverallAvgServiceMin(18.45);
        m.setOverallAvgVisitDurationMin(68.5);
        return m;
    }
}
