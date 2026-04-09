package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.GenerationTaskStatus;
import com.codehaja.domain.generation.entity.LectureContentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureContentTaskRepository extends JpaRepository<LectureContentTask, Long> {

    List<LectureContentTask> findByJobIdOrderBySectionIdAscLectureIdAsc(Long jobId);

    List<LectureContentTask> findByJobIdAndStatus(Long jobId, GenerationTaskStatus status);

    List<LectureContentTask> findByJobIdAndStatusIn(Long jobId, List<GenerationTaskStatus> statuses);

    long countByJobIdAndStatus(Long jobId, GenerationTaskStatus status);

    long countByJobIdAndStatusIn(Long jobId, List<GenerationTaskStatus> statuses);

    Optional<LectureContentTask> findTopByLectureIdOrderByIdDesc(Long lectureId);

    void deleteAllByJobId(Long jobId);
}
