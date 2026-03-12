package com.codehaja.domain.section.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.course.repository.CourseRepository;
import com.codehaja.domain.section.dto.CourseSectionDto;
import com.codehaja.domain.section.entity.CourseSection;
import com.codehaja.domain.section.mapper.CourseSectionMapper;
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
public class CourseSectionService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CourseSectionMapper courseSectionMapper;

    @Transactional
    public CourseSectionDto.DetailResponse createSection(Long courseId, CourseSectionDto.CreateRequest request) {
        validateCreateRequest(request);

        Course course = getCourseOrThrow(courseId);

        CourseSection section = courseSectionMapper.toEntity(request);
        section.setCourse(course);

        applyDefaultsOnCreate(courseId, section);

        CourseSection saved = courseSectionRepository.save(section);
        return courseSectionMapper.toDetailResponse(saved);
    }

    public Page<CourseSectionDto.SummaryResponse> getSections(
            Long courseId,
            int page,
            int size,
            String keyword
    ) {
        getCourseOrThrow(courseId);

        int safePage = Math.max(page, 0);
        int safeSize = normalizePageSize(size);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        return courseSectionRepository.searchSections(courseId, keyword, pageable);
    }

    public CourseSectionDto.DetailResponse getSection(Long sectionId) {
        CourseSection section = courseSectionRepository.findWithCourseById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        return courseSectionMapper.toDetailResponse(section);
    }

    @Transactional
    public CourseSectionDto.DetailResponse updateSection(Long sectionId, CourseSectionDto.UpdateRequest request) {
        validateUpdateRequest(request);

        CourseSection section = courseSectionRepository.findWithCourseById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        courseSectionMapper.updateEntityFromDto(request, section);
        applyDefaultsOnUpdate(section);

        return courseSectionMapper.toDetailResponse(section);
    }

    @Transactional
    public void deleteSection(Long sectionId) {
        CourseSection section = courseSectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        courseSectionRepository.delete(section);
    }

    @Transactional
    public List<CourseSectionDto.SummaryResponse> reorderSections(
            Long courseId,
            List<CourseSectionDto.ReorderRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.SECTION_REORDER_INVALID, "Reorder request is empty.");
        }

        getCourseOrThrow(courseId);

        List<CourseSection> sections = courseSectionRepository.findAllByCourseId(courseId);
        if (sections.isEmpty()) {
            throw new BusinessException(ErrorCode.SECTION_NOT_FOUND, "No sections found for this course.");
        }

        Map<Long, CourseSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(CourseSection::getId, Function.identity()));

        Set<Long> requestIds = new HashSet<>();
        for (CourseSectionDto.ReorderRequest request : requests) {
            if (request.getId() == null || request.getSortOrder() == null) {
                throw new BusinessException(ErrorCode.SECTION_REORDER_INVALID, "Section id and sortOrder are required.");
            }

            if (!sectionMap.containsKey(request.getId())) {
                throw new BusinessException(ErrorCode.SECTION_REORDER_INVALID, "Section does not belong to this course.");
            }

            if (!requestIds.add(request.getId())) {
                throw new BusinessException(ErrorCode.SECTION_REORDER_INVALID, "Duplicate section id in reorder request.");
            }
        }

        for (CourseSectionDto.ReorderRequest request : requests) {
            CourseSection section = sectionMap.get(request.getId());
            section.setSortOrder(request.getSortOrder());
        }

        List<CourseSection> updated = courseSectionRepository.findAllByCourseIdOrderBySortOrderAsc(courseId);

        return updated.stream()
                .map(courseSectionMapper::toSummaryResponse)
                .toList();
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    private void validateCreateRequest(CourseSectionDto.CreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Section title is required.");
        }
    }

    private void validateUpdateRequest(CourseSectionDto.UpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Section title is required.");
        }
    }

    private void applyDefaultsOnCreate(Long courseId, CourseSection section) {
        if (section.getHours() == null) {
            section.setHours(0);
        }
        if (section.getPoints() == null) {
            section.setPoints(0);
        }
        if (section.getSortOrder() == null || section.getSortOrder() <= 0) {
            Integer maxSortOrder = courseSectionRepository.findMaxSortOrderByCourseId(courseId);
            section.setSortOrder((maxSortOrder == null ? 0 : maxSortOrder) + 1);
        }
    }

    private void applyDefaultsOnUpdate(CourseSection section) {
        if (section.getHours() == null) {
            section.setHours(0);
        }
        if (section.getPoints() == null) {
            section.setPoints(0);
        }
        if (section.getSortOrder() == null || section.getSortOrder() <= 0) {
            section.setSortOrder(1);
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