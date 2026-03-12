package com.codehaja.domain.lectureitementry.repository;

import com.codehaja.domain.lectureitementry.dto.LectureItemEntryDto;
import com.codehaja.domain.lectureitementry.entity.AccessLevel;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LectureItemEntryQueryRepository {
    Page<LectureItemEntryDto.SummaryResponse> searchEntries(
            Long lectureItemId,
            String keyword,
            LectureItemEntryType entryType,
            AccessLevel accessLevel,
            Boolean isActive,
            Pageable pageable
    );
}