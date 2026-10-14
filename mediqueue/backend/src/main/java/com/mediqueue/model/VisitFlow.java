package com.mediqueue.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "visit_flows")
public class VisitFlow {

    @Id
    private String id;
    private String visitId;
    private String patientId;
    private String ageGroup;
    private String gender;
    private String admissionType;
    private String visitDate;
    private int totalStagesCount;
    private int cumulativeWaitMin;
    private int cumulativeServiceMin;
    private int totalJourneyDurationMin;
    private List<StageItem> stages;

    public static class StageItem {
        private int stageSequence;
        private String stageName;
        private String departmentName;
        private String arrivalTs;
        private String serviceStartTs;
        private String serviceEndTs;
        private int waitTimeMin;
        private int serviceDurMin;
        private int totalStageTimeMin;

        public StageItem() {}

        public int getStageSequence() { return stageSequence; }
        public void setStageSequence(int stageSequence) { this.stageSequence = stageSequence; }

        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }

        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

        public String getArrivalTs() { return arrivalTs; }
        public void setArrivalTs(String arrivalTs) { this.arrivalTs = arrivalTs; }

        public String getServiceStartTs() { return serviceStartTs; }
        public void setServiceStartTs(String serviceStartTs) { this.serviceStartTs = serviceStartTs; }

        public String getServiceEndTs() { return serviceEndTs; }
        public void setServiceEndTs(String serviceEndTs) { this.serviceEndTs = serviceEndTs; }

        public int getWaitTimeMin() { return waitTimeMin; }
        public void setWaitTimeMin(int waitTimeMin) { this.waitTimeMin = waitTimeMin; }

        public int getServiceDurMin() { return serviceDurMin; }
        public void setServiceDurMin(int serviceDurMin) { this.serviceDurMin = serviceDurMin; }

        public int getTotalStageTimeMin() { return totalStageTimeMin; }
        public void setTotalStageTimeMin(int totalStageTimeMin) { this.totalStageTimeMin = totalStageTimeMin; }
    }

    public VisitFlow() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVisitId() { return visitId; }
    public void setVisitId(String visitId) { this.visitId = visitId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public int getTotalStagesCount() { return totalStagesCount; }
    public void setTotalStagesCount(int totalStagesCount) { this.totalStagesCount = totalStagesCount; }

    public int getCumulativeWaitMin() { return cumulativeWaitMin; }
    public void setCumulativeWaitMin(int cumulativeWaitMin) { this.cumulativeWaitMin = cumulativeWaitMin; }

    public int getCumulativeServiceMin() { return cumulativeServiceMin; }
    public void setCumulativeServiceMin(int cumulativeServiceMin) { this.cumulativeServiceMin = cumulativeServiceMin; }

    public int getTotalJourneyDurationMin() { return totalJourneyDurationMin; }
    public void setTotalJourneyDurationMin(int totalJourneyDurationMin) { this.totalJourneyDurationMin = totalJourneyDurationMin; }

    public List<StageItem> getStages() { return stages; }
    public void setStages(List<StageItem> stages) { this.stages = stages; }
}
