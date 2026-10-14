package com.mediqueue.repository;

import com.mediqueue.model.StageFlow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StageFlowRepository extends MongoRepository<StageFlow, String> {
    List<StageFlow> findAllByOrderBySequenceAsc();
}
