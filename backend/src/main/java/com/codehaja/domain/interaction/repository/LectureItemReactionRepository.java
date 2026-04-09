package com.codehaja.domain.interaction.repository;

import com.codehaja.domain.interaction.entity.LectureItemReaction;
import com.codehaja.domain.interaction.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureItemReactionRepository extends JpaRepository<LectureItemReaction, Long> {

    Optional<LectureItemReaction> findByUserIdAndLectureItemId(Long userId, Long lectureItemId);

    long countByLectureItemIdAndReactionType(Long lectureItemId, ReactionType reactionType);
}
