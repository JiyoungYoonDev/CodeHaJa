package com.codehaja.domain.coding.repository;

import com.codehaja.domain.coding.entity.CodingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {
    List<CodingSubmission> findAllByAnonymousUserIdAndLectureItemEntryIdOrderByCreatedAtDesc(Long anonymousUserId, Long lectureItemEntryId);
}