package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.CourseGenerationJob;
import com.codehaja.domain.generation.entity.GenerationJobStatus;
import com.codehaja.domain.generation.repository.CourseGenerationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages CourseGenerationJob lifecycle in its own transaction.
 * REQUIRES_NEW ensures the job row is committed and visible to
 * other REQUIRES_NEW transactions (e.g. GenerationOutputLogger).
 */
@Service
@RequiredArgsConstructor
public class GenerationJobManager {

    private final CourseGenerationJobRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseGenerationJob createJob(String topic, String modelName) {
        CourseGenerationJob job = new CourseGenerationJob();
        job.setTopic(topic);
        job.setModelName(modelName);
        job.setStatus(GenerationJobStatus.IN_PROGRESS);
        job.setStructuredOutputUsed(true);
        job.setStartedAt(LocalDateTime.now());
        return repository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJob(CourseGenerationJob job) {
        repository.save(job);
    }
}
