package com.mediqueue.repository;

import com.mediqueue.model.VisitFlow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitFlowRepository extends MongoRepository<VisitFlow, String> {
    Optional<VisitFlow> findByVisitId(String visitId);
    List<VisitFlow> findTop20ByOrderByTotalJourneyDurationMinDesc();
    List<VisitFlow> findByAdmissionType(String admissionType);
}
