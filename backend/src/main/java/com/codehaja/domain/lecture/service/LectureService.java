package com.codehaja.domain.lecture.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.lecture.dto.LectureDto;
import com.codehaja.domain.lecture.entity.Lecture;
import com.codehaja.domain.lecture.mapper.LectureMapper;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.section.entity.CourseSection;
import com.codehaja.domain.section.repository.CourseSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

    private final LectureRepository lectureRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LectureItemRepository lectureItemRepository;
    private final LectureMapper lectureMapper;

    @Transactional
    public LectureDto.DetailResponse createLecture(Long courseSectionId, LectureDto.CreateRequest request) {
        validateCreateRequest(request);

        CourseSection courseSection = getSectionOrThrow(courseSectionId);

        Lecture lecture = lectureMapper.toEntity(request);
        lecture.setCourseSection(courseSection);

        applyDefaultsOnCreate(courseSectionId, lecture);

        Lecture saved = lectureRepository.save(lecture);
        return lectureMapper.toDetailResponse(saved);
    }

    public Page<LectureDto.SummaryResponse> getLectures(
            Long courseSectionId,
            int page,
            int size,
            String keyword,
            Boolean isPublished,
            Boolean isPreview
    ) {
        getSectionOrThrow(courseSectionId);

        int safePage = Math.max(page, 0);
        int safeSize = normalizePageSize(size);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<LectureDto.SummaryResponse> result =
                lectureRepository.searchLectures(courseSectionId, keyword, isPublished, isPreview, pageable);

        result.getContent().forEach(lr ->
                lectureItemRepository.findFirstByLectureIdOrderBySortOrderAsc(lr.getId())
                        .ifPresent(item -> lr.setFirstItemId(item.getId()))
        );

        return result;
    }

    public LectureDto.DetailResponse getLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findWithSectionAndCourseById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        return lectureMapper.toDetailResponse(lecture);
    }

    @Transactional
    public LectureDto.DetailResponse updateLecture(Long lectureId, LectureDto.UpdateRequest request) {
        validateUpdateRequest(request);

        Lecture lecture = lectureRepository.findWithSectionAndCourseById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        lectureMapper.updateEntityFromDto(request, lecture);
        applyDefaultsOnUpdate(lecture);

        return lectureMapper.toDetailResponse(lecture);
    }

    @Transactional
    public LectureDto.DetailResponse updatePreview(Long lectureId, Boolean isPreview) {
        Lecture lecture = lectureRepository.findWithSectionAndCourseById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (isPreview == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "isPreview is required.");
        }

        lecture.setIsPreview(isPreview);
        return lectureMapper.toDetailResponse(lecture);
    }

    @Transactional
    public LectureDto.DetailResponse updatePublish(Long lectureId, Boolean isPublished) {
        Lecture lecture = lectureRepository.findWithSectionAndCourseById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (isPublished == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "isPublished is required.");
        }

        lecture.setIsPublished(isPublished);
        return lectureMapper.toDetailResponse(lecture);
    }

    @Transactional
    public void deleteLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        lectureRepository.delete(lecture);
    }

    @Transactional
    public List<LectureDto.SummaryResponse> reorderLectures(
            Long courseSectionId,
            List<LectureDto.ReorderRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.LECTURE_REORDER_INVALID, "Reorder request is empty.");
        }

        getSectionOrThrow(courseSectionId);

        List<Lecture> lectures = lectureRepository.findAllByCourseSectionId(courseSectionId);
        if (lectures.isEmpty()) {
            throw new BusinessException(ErrorCode.LECTURE_NOT_FOUND, "No lectures found for this section.");
        }

        Map<Long, Lecture> lectureMap = lectures.stream()
                .collect(Collectors.toMap(Lecture::getId, Function.identity()));

        Set<Long> requestIds = new HashSet<>();
        for (LectureDto.ReorderRequest request : requests) {
            if (request.getId() == null || request.getSortOrder() == null) {
                throw new BusinessException(ErrorCode.LECTURE_REORDER_INVALID, "Lecture id and sortOrder are required.");
            }

            if (!lectureMap.containsKey(request.getId())) {
                throw new BusinessException(ErrorCode.LECTURE_REORDER_INVALID, "Lecture does not belong to this section.");
            }

            if (!requestIds.add(request.getId())) {
                throw new BusinessException(ErrorCode.LECTURE_REORDER_INVALID, "Duplicate lecture id in reorder request.");
            }
        }

        for (LectureDto.ReorderRequest request : requests) {
            Lecture lecture = lectureMap.get(request.getId());
            lecture.setSortOrder(request.getSortOrder());
        }

        List<Lecture> updated = lectureRepository.findAllByCourseSectionIdOrderBySortOrderAsc(courseSectionId);

        return updated.stream()
                .map(lectureMapper::toSummaryResponse)
                .toList();
    }

    private CourseSection getSectionOrThrow(Long courseSectionId) {
        return courseSectionRepository.findWithCourseById(courseSectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));
    }

    private void validateCreateRequest(LectureDto.CreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture title is required.");
        }

        if (request.getLectureType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture type is required.");
        }
    }

    private void validateUpdateRequest(LectureDto.UpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture title is required.");
        }

        if (request.getLectureType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture type is required.");
        }
    }

    private void applyDefaultsOnCreate(Long courseSectionId, Lecture lecture) {
        if (lecture.getDurationMinutes() == null) {
            lecture.setDurationMinutes(0);
        }
        if (lecture.getIsPreview() == null) {
            lecture.setIsPreview(false);
        }
        if (lecture.getIsPublished() == null) {
            lecture.setIsPublished(false);
        }
        if (lecture.getSortOrder() == null || lecture.getSortOrder() <= 0) {
            Integer maxSortOrder = lectureRepository.findMaxSortOrderByCourseSectionId(courseSectionId);
            lecture.setSortOrder((maxSortOrder == null ? 0 : maxSortOrder) + 1);
        }
    }

    private void applyDefaultsOnUpdate(Lecture lecture) {
        if (lecture.getDurationMinutes() == null) {
            lecture.setDurationMinutes(0);
        }
        if (lecture.getIsPreview() == null) {
            lecture.setIsPreview(false);
        }
        if (lecture.getIsPublished() == null) {
            lecture.setIsPublished(false);
        }
        if (lecture.getSortOrder() == null || lecture.getSortOrder() <= 0) {
            lecture.setSortOrder(1);
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}