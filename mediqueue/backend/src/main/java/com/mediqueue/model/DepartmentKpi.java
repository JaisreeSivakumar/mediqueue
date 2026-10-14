package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "department_kpis")
public class DepartmentKpi {

    @Id
    private String id;
    private int departmentId;
    private String departmentCode;
    private String departmentName;
    private int totalVisitsServed;
    private double avgWaitTimeMin;
    private double medianWaitTimeMin;
    private int minWaitTimeMin;
    private int maxWaitTimeMin;
    private double p90WaitTimeMin;
    private double avgServiceDurMin;
    private double avgStageTotalMin;
    private int targetWaitMin;
    private double pctExceedingTarget;
    private String status;
    private int waitTimeRank;

    public DepartmentKpi() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getTotalVisitsServed() { return totalVisitsServed; }
    public void setTotalVisitsServed(int totalVisitsServed) { this.totalVisitsServed = totalVisitsServed; }

    public double getAvgWaitTimeMin() { return avgWaitTimeMin; }
    public void setAvgWaitTimeMin(double avgWaitTimeMin) { this.avgWaitTimeMin = avgWaitTimeMin; }

    public double getMedianWaitTimeMin() { return medianWaitTimeMin; }
    public void setMedianWaitTimeMin(double medianWaitTimeMin) { this.medianWaitTimeMin = medianWaitTimeMin; }

    public int getMinWaitTimeMin() { return minWaitTimeMin; }
    public void setMinWaitTimeMin(int minWaitTimeMin) { this.minWaitTimeMin = minWaitTimeMin; }

    public int getMaxWaitTimeMin() { return maxWaitTimeMin; }
    public void setMaxWaitTimeMin(int maxWaitTimeMin) { this.maxWaitTimeMin = maxWaitTimeMin; }

    public double getP90WaitTimeMin() { return p90WaitTimeMin; }
    public void setP90WaitTimeMin(double p90WaitTimeMin) { this.p90WaitTimeMin = p90WaitTimeMin; }

    public double getAvgServiceDurMin() { return avgServiceDurMin; }
    public void setAvgServiceDurMin(double avgServiceDurMin) { this.avgServiceDurMin = avgServiceDurMin; }

    public double getAvgStageTotalMin() { return avgStageTotalMin; }
    public void setAvgStageTotalMin(double avgStageTotalMin) { this.avgStageTotalMin = avgStageTotalMin; }

    public int getTargetWaitMin() { return targetWaitMin; }
    public void setTargetWaitMin(int targetWaitMin) { this.targetWaitMin = targetWaitMin; }

    public double getPctExceedingTarget() { return pctExceedingTarget; }
    public void setPctExceedingTarget(double pctExceedingTarget) { this.pctExceedingTarget = pctExceedingTarget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getWaitTimeRank() { return waitTimeRank; }
    public void setWaitTimeRank(int waitTimeRank) { this.waitTimeRank = waitTimeRank; }
}
