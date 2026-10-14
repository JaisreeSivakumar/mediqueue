package com.mediqueue.repository;

import com.mediqueue.model.PipelineMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PipelineMetadataRepository extends MongoRepository<PipelineMetadata, String> {
    Optional<PipelineMetadata> findTopByOrderByLastRunTimestampDesc();
}
