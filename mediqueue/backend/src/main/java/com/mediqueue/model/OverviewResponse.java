package com.mediqueue.model;

import java.util.List;

public class OverviewResponse {

    private long totalVisits;
    private long totalStages;
    private int totalDepartments;
    private double avgWaitTimeMin;
    private double avgServiceDurationMin;
    private double avgTotalVisitDurationMin;
    private int bottlenecksCount;
    private String peakCongestionPeriod;
    private PipelineMetadata pipelineInfo;
    private List<DepartmentKpi> topCongestedDepartments;
    private List<HourlyCongestion> hourlyTrends;
    private List<StageFlow> flowSummary;

    public OverviewResponse() {}

    public long getTotalVisits() { return totalVisits; }
    public void setTotalVisits(long totalVisits) { this.totalVisits = totalVisits; }

    public long getTotalStages() { return totalStages; }
    public void setTotalStages(long totalStages) { this.totalStages = totalStages; }

    public int getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(int totalDepartments) { this.totalDepartments = totalDepartments; }

    public double getAvgWaitTimeMin() { return avgWaitTimeMin; }
    public void setAvgWaitTimeMin(double avgWaitTimeMin) { this.avgWaitTimeMin = avgWaitTimeMin; }

    public double getAvgServiceDurationMin() { return avgServiceDurationMin; }
    public void setAvgServiceDurationMin(double avgServiceDurationMin) { this.avgServiceDurationMin = avgServiceDurationMin; }

    public double getAvgTotalVisitDurationMin() { return avgTotalVisitDurationMin; }
    public void setAvgTotalVisitDurationMin(double avgTotalVisitDurationMin) { this.avgTotalVisitDurationMin = avgTotalVisitDurationMin; }

    public int getBottlenecksCount() { return bottlenecksCount; }
    public void setBottlenecksCount(int bottlenecksCount) { this.bottlenecksCount = bottlenecksCount; }

    public String getPeakCongestionPeriod() { return peakCongestionPeriod; }
    public void setPeakCongestionPeriod(String peakCongestionPeriod) { this.peakCongestionPeriod = peakCongestionPeriod; }

    public PipelineMetadata getPipelineInfo() { return pipelineInfo; }
    public void setPipelineInfo(PipelineMetadata pipelineInfo) { this.pipelineInfo = pipelineInfo; }

    public List<DepartmentKpi> getTopCongestedDepartments() { return topCongestedDepartments; }
    public void setTopCongestedDepartments(List<DepartmentKpi> topCongestedDepartments) { this.topCongestedDepartments = topCongestedDepartments; }

    public List<HourlyCongestion> getHourlyTrends() { return hourlyTrends; }
    public void setHourlyTrends(List<HourlyCongestion> hourlyTrends) { this.hourlyTrends = hourlyTrends; }

    public List<StageFlow> getFlowSummary() { return flowSummary; }
    public void setFlowSummary(List<StageFlow> flowSummary) { this.flowSummary = flowSummary; }
}
