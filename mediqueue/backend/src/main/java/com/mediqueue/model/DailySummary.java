package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "daily_summaries")
public class DailySummary {

    @Id
    private String id;
    private String visitDate;
    private int uniqueVisits;
    private int totalStages;
    private double avgWaitTimeMin;
    private double avgServiceDurMin;

    public DailySummary() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public int getUniqueVisits() { return uniqueVisits; }
    public void setUniqueVisits(int uniqueVisits) { this.uniqueVisits = uniqueVisits; }

    public int getTotalStages() { return totalStages; }
    public void setTotalStages(int totalStages) { this.totalStages = totalStages; }

    public double getAvgWaitTimeMin() { return avgWaitTimeMin; }
    public void setAvgWaitTimeMin(double avgWaitTimeMin) { this.avgWaitTimeMin = avgWaitTimeMin; }

    public double getAvgServiceDurMin() { return avgServiceDurMin; }
    public void setAvgServiceDurMin(double avgServiceDurMin) { this.avgServiceDurMin = avgServiceDurMin; }
}
