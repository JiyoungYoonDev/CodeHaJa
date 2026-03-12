package com.codehaja.domain.lecture.repository;

import com.codehaja.domain.lecture.dto.LectureDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LectureQueryRepository {
    Page<LectureDto.SummaryResponse> searchLectures(
            Long courseSectionId,
            String keyword,
            Boolean isPublished,
            Boolean isPreview,
            Pageable pageable
    );
}