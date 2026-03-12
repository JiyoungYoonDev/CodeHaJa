package com.codehaja.domain.section.repository;

import com.codehaja.domain.section.dto.CourseSectionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseSectionQueryRepository {
    Page<CourseSectionDto.SummaryResponse> searchSections(Long courseId, String keyword, Pageable pageable);
}