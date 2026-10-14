package com.mediqueue.service;

import com.mediqueue.model.BottleneckReport;
import com.mediqueue.model.DailySummary;
import com.mediqueue.model.DepartmentKpi;
import com.mediqueue.model.HourlyCongestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ReportExportService {

    @Autowired
    private AnalyticsService analyticsService;

    public ByteArrayInputStream exportDepartmentsCsv() {
        List<DepartmentKpi> list = analyticsService.getAllDepartments();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Rank,Department ID,Code,Department Name,Visits Served,Avg Wait (min),Median Wait (min),Min Wait,Max Wait,P90 Wait,Avg Service (min),Target Wait (min),% Exceeding Target,Status");

        for (DepartmentKpi d : list) {
            writer.printf("%d,%d,%s,\"%s\",%d,%.2f,%.2f,%d,%d,%.2f,%.2f,%d,%.2f,%s\n",
                    d.getWaitTimeRank(), d.getDepartmentId(), d.getDepartmentCode(), d.getDepartmentName(),
                    d.getTotalVisitsServed(), d.getAvgWaitTimeMin(), d.getMedianWaitTimeMin(),
                    d.getMinWaitTimeMin(), d.getMaxWaitTimeMin(), d.getP90WaitTimeMin(),
                    d.getAvgServiceDurMin(), d.getTargetWaitMin(), d.getPctExceedingTarget(), d.getStatus());
        }
        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportBottlenecksCsv() {
        List<BottleneckReport> list = analyticsService.getBottlenecks("ALL", null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Bottleneck ID,Department Name,Stage Name,Hour Window,Affected Visits,Avg Wait (min),Peak Wait (min),Target Wait (min),Congestion Multiplier,Severity,Days Observed,Rationale");

        for (BottleneckReport b : list) {
            writer.printf("%s,\"%s\",%s,%s,%d,%.2f,%d,%d,%.2f,%s,%d,\"%s\"\n",
                    b.getBottleneckId(), b.getDepartmentName(), b.getStageName(), b.getHourWindow(),
                    b.getAffectedVisits(), b.getAvgWaitTimeMin(), b.getPeakWaitTimeMin(),
                    b.getTargetWaitMin(), b.getCongestionMultiplier(), b.getSeverity(),
                    b.getDistinctDaysObserved(), b.getRationale().replace("\"", "\"\""));
        }
        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportHourlyCongestionCsv() {
        List<HourlyCongestion> list = analyticsService.getHourlyCongestion();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Hour,Time Window,Total Patients Arrived,Avg Wait Time (min),Avg Service Duration (min),% Severely Congested (>30m)");

        for (HourlyCongestion h : list) {
            writer.printf("%d,%s,%d,%.2f,%.2f,%.2f\n",
                    h.getArrivalHour(), h.getHourLabel(), h.getTotalPatients(),
                    h.getAvgWaitTimeMin(), h.getAvgServiceDurMin(), h.getPctCongested());
        }
        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportDailyTrendsCsv() {
        List<DailySummary> list = analyticsService.getDailySummaries();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.println("Visit Date,Unique Patient Visits,Total Stages Completed,Avg Queue Wait Time (min),Avg Service Duration (min)");

        for (DailySummary d : list) {
            writer.printf("%s,%d,%d,%.2f,%.2f\n",
                    d.getVisitDate(), d.getUniqueVisits(), d.getTotalStages(),
                    d.getAvgWaitTimeMin(), d.getAvgServiceDurMin());
        }
        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }
}
