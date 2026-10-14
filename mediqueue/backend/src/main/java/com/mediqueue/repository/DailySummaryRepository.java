package com.mediqueue.repository;

import com.mediqueue.model.DailySummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DailySummaryRepository extends MongoRepository<DailySummary, String> {
    List<DailySummary> findAllByOrderByVisitDateAsc();
}
