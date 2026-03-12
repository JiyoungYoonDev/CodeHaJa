package com.codehaja.domain.lectureitem.repository;

import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LectureItemQueryRepository {
    Page<LectureItemDto.SummaryResponse> searchLectureItems(
            Long lectureId,
            String keyword,
            LectureItemType itemType,
            Pageable pageable
    );
}