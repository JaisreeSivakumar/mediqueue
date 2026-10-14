package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pipeline_metadata")
public class PipelineMetadata {

    @Id
    private String id;
    private String pipelineVersion;
    private String lastRunTimestamp;
    private int totalVisitsAnalyzed;
    private int totalStagesCleaned;
    private int totalDepartments;
    private int bottlenecksDetected;
    private String peakHour;
    private double overallAvgWaitMin;
    private double overallAvgServiceMin;
    private double overallAvgVisitDurationMin;

    public PipelineMetadata() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPipelineVersion() { return pipelineVersion; }
    public void setPipelineVersion(String pipelineVersion) { this.pipelineVersion = pipelineVersion; }

    public String getLastRunTimestamp() { return lastRunTimestamp; }
    public void setLastRunTimestamp(String lastRunTimestamp) { this.lastRunTimestamp = lastRunTimestamp; }

    public int getTotalVisitsAnalyzed() { return totalVisitsAnalyzed; }
    public void setTotalVisitsAnalyzed(int totalVisitsAnalyzed) { this.totalVisitsAnalyzed = totalVisitsAnalyzed; }

    public int getTotalStagesCleaned() { return totalStagesCleaned; }
    public void setTotalStagesCleaned(int totalStagesCleaned) { this.totalStagesCleaned = totalStagesCleaned; }

    public int getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(int totalDepartments) { this.totalDepartments = totalDepartments; }

    public int getBottlenecksDetected() { return bottlenecksDetected; }
    public void setBottlenecksDetected(int bottlenecksDetected) { this.bottlenecksDetected = bottlenecksDetected; }

    public String getPeakHour() { return peakHour; }
    public void setPeakHour(String peakHour) { this.peakHour = peakHour; }

    public double getOverallAvgWaitMin() { return overallAvgWaitMin; }
    public void setOverallAvgWaitMin(double overallAvgWaitMin) { this.overallAvgWaitMin = overallAvgWaitMin; }

    public double getOverallAvgServiceMin() { return overallAvgServiceMin; }
    public void setOverallAvgServiceMin(double overallAvgServiceMin) { this.overallAvgServiceMin = overallAvgServiceMin; }

    public double getOverallAvgVisitDurationMin() { return overallAvgVisitDurationMin; }
    public void setOverallAvgVisitDurationMin(double overallAvgVisitDurationMin) { this.overallAvgVisitDurationMin = overallAvgVisitDurationMin; }
}
