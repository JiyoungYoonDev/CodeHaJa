package com.codehaja.domain.interaction.repository;

import com.codehaja.domain.interaction.entity.HintUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HintUsageRepository extends JpaRepository<HintUsage, Long> {

    List<HintUsage> findByUserIdAndLectureItemId(Long userId, Long lectureItemId);

    int countByUserIdAndLectureItemId(Long userId, Long lectureItemId);

    boolean existsByUserIdAndLectureItemIdAndHintIndex(Long userId, Long lectureItemId, Integer hintIndex);
}
