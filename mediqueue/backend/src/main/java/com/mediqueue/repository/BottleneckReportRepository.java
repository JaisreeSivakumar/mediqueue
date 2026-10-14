package com.mediqueue.repository;

import com.mediqueue.model.BottleneckReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BottleneckReportRepository extends MongoRepository<BottleneckReport, String> {
    List<BottleneckReport> findAllByOrderByAvgWaitTimeMinDesc();
    List<BottleneckReport> findBySeverityOrderByAvgWaitTimeMinDesc(String severity);
    List<BottleneckReport> findByDepartmentId(int departmentId);
}
