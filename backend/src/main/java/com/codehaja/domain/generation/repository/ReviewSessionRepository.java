package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.ReviewAction;
import com.codehaja.domain.generation.entity.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewSessionRepository extends JpaRepository<ReviewSession, Long> {

    List<ReviewSession> findByJobIdOrderByReviewedAtDesc(Long jobId);

    long countByJobIdAndAction(Long jobId, ReviewAction action);
}
