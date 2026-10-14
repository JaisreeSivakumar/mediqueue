package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stage_flows")
public class StageFlow {

    @Id
    private String id;
    private int sequence;
    private String stageName;
    private int totalPatientsProcessed;
    private int totalStageEvents;
    private double avgQueueWaitMin;
    private double avgServiceDurMin;
    private double avgTotalStageMin;

    public StageFlow() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public int getTotalPatientsProcessed() { return totalPatientsProcessed; }
    public void setTotalPatientsProcessed(int totalPatientsProcessed) { this.totalPatientsProcessed = totalPatientsProcessed; }

    public int getTotalStageEvents() { return totalStageEvents; }
    public void setTotalStageEvents(int totalStageEvents) { this.totalStageEvents = totalStageEvents; }

    public double getAvgQueueWaitMin() { return avgQueueWaitMin; }
    public void setAvgQueueWaitMin(double avgQueueWaitMin) { this.avgQueueWaitMin = avgQueueWaitMin; }

    public double getAvgServiceDurMin() { return avgServiceDurMin; }
    public void setAvgServiceDurMin(double avgServiceDurMin) { this.avgServiceDurMin = avgServiceDurMin; }

    public double getAvgTotalStageMin() { return avgTotalStageMin; }
    public void setAvgTotalStageMin(double avgTotalStageMin) { this.avgTotalStageMin = avgTotalStageMin; }
}
