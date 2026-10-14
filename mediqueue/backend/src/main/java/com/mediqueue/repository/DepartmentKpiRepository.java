package com.mediqueue.repository;

import com.mediqueue.model.DepartmentKpi;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentKpiRepository extends MongoRepository<DepartmentKpi, String> {
    List<DepartmentKpi> findAllByOrderByAvgWaitTimeMinDesc();
    Optional<DepartmentKpi> findByDepartmentId(int departmentId);
}
