package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bottleneck_reports")
public class BottleneckReport {

    @Id
    private String id;
    private String bottleneckId;
    private int departmentId;
    private String departmentName;
    private String stageName;
    private int arrivalHour;
    private String hourWindow;
    private int affectedVisits;
    private double avgWaitTimeMin;
    private int peakWaitTimeMin;
    private int targetWaitMin;
    private double congestionMultiplier;
    private String severity; // CRITICAL, HIGH, MODERATE
    private int distinctDaysObserved;
    private String rationale;

    public BottleneckReport() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBottleneckId() { return bottleneckId; }
    public void setBottleneckId(String bottleneckId) { this.bottleneckId = bottleneckId; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public int getArrivalHour() { return arrivalHour; }
    public void setArrivalHour(int arrivalHour) { this.arrivalHour = arrivalHour; }

    public String getHourWindow() { return hourWindow; }
    public void setHourWindow(String hourWindow) { this.hourWindow = hourWindow; }

    public int getAffectedVisits() { return affectedVisits; }
    public void setAffectedVisits(int affectedVisits) { this.affectedVisits = affectedVisits; }

    public double getAvgWaitTimeMin() { return avgWaitTimeMin; }
    public void setAvgWaitTimeMin(double avgWaitTimeMin) { this.avgWaitTimeMin = avgWaitTimeMin; }

    public int getPeakWaitTimeMin() { return peakWaitTimeMin; }
    public void setPeakWaitTimeMin(int peakWaitTimeMin) { this.peakWaitTimeMin = peakWaitTimeMin; }

    public int getTargetWaitMin() { return targetWaitMin; }
    public void setTargetWaitMin(int targetWaitMin) { this.targetWaitMin = targetWaitMin; }

    public double getCongestionMultiplier() { return congestionMultiplier; }
    public void setCongestionMultiplier(double congestionMultiplier) { this.congestionMultiplier = congestionMultiplier; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public int getDistinctDaysObserved() { return distinctDaysObserved; }
    public void setDistinctDaysObserved(int distinctDaysObserved) { this.distinctDaysObserved = distinctDaysObserved; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
