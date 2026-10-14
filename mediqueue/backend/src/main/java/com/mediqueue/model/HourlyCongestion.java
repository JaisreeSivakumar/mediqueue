package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "hourly_congestion")
public class HourlyCongestion {

    @Id
    private String id;
    private int arrivalHour;
    private String hourLabel;
    private int totalPatients;
    private double avgWaitTimeMin;
    private double avgServiceDurMin;
    private double pctCongested;

    public HourlyCongestion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getArrivalHour() { return arrivalHour; }
    public void setArrivalHour(int arrivalHour) { this.arrivalHour = arrivalHour; }

    public String getHourLabel() { return hourLabel; }
    public void setHourLabel(String hourLabel) { this.hourLabel = hourLabel; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public double getAvgWaitTimeMin() { return avgWaitTimeMin; }
    public void setAvgWaitTimeMin(double avgWaitTimeMin) { this.avgWaitTimeMin = avgWaitTimeMin; }

    public double getAvgServiceDurMin() { return avgServiceDurMin; }
    public void setAvgServiceDurMin(double avgServiceDurMin) { this.avgServiceDurMin = avgServiceDurMin; }

    public double getPctCongested() { return pctCongested; }
    public void setPctCongested(double pctCongested) { this.pctCongested = pctCongested; }
}
