package com.codehaja.domain.coding.repository;

import com.codehaja.domain.coding.entity.CodingDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodingDraftRepository extends JpaRepository<CodingDraft, Long> {
    Optional<CodingDraft> findByAnonymousUserIdAndLectureItemEntryId(Long anonymousUserId, Long lectureItemEntryId);
}