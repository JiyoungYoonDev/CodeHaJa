package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.GenerationTaskStatus;
import com.codehaja.domain.generation.entity.LectureContentBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureContentBatchRepository extends JpaRepository<LectureContentBatch, Long> {

    List<LectureContentBatch> findByTaskIdOrderByBatchIndexAsc(Long taskId);

    List<LectureContentBatch> findByTaskIdAndStatusIn(Long taskId, List<GenerationTaskStatus> statuses);

    long countByTaskIdAndStatus(Long taskId, GenerationTaskStatus status);

    /** Leaf batches only — used for aggregation (excludes SPLIT parents). */
    List<LectureContentBatch> findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(Long taskId);

    /** Child batches created from a SPLIT parent. */
    List<LectureContentBatch> findByParentBatchIdOrderByBatchIndexAsc(Long parentBatchId);
}
