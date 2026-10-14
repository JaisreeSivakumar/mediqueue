package com.mediqueue.repository;

import com.mediqueue.model.HourlyCongestion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HourlyCongestionRepository extends MongoRepository<HourlyCongestion, String> {
    List<HourlyCongestion> findAllByOrderByArrivalHourAsc();
}
