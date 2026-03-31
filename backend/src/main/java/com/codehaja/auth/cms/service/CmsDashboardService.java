package com.codehaja.auth.cms.service;

import com.codehaja.auth.cms.dto.CmsDashboardDto;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.domain.category.repository.CourseCategoryRepository;
import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.repository.CourseRepository;
import com.codehaja.domain.enrollment.repository.CourseEnrollmentRepository;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import com.codehaja.domain.lectureitem.mapper.LectureItemMapper;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.section.repository.CourseSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsDashboardService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseCategoryRepository categoryRepository;
    private final CourseSectionRepository sectionRepository;
    private final LectureRepository lectureRepository;
    private final LectureItemRepository lectureItemRepository;
    private final LectureItemMapper lectureItemMapper;

    public CmsDashboardDto.StatsResponse getStats() {
        List<CmsDashboardDto.RecentCourse> recentCourses = courseRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(6)
                .map(c -> {
                    String categoryName = c.getCategory() != null ? c.getCategory().getCategoryName() : null;
                    return CmsDashboardDto.RecentCourse.builder()
                            .id(c.getId())
                            .title(c.getTitle())
                            .status(c.getStatus().name())
                            .difficulty(c.getDifficulty().name())
                            .category(categoryName)
                            .createdAt(c.getCreatedAt())
                            .build();
                })
                .toList();

        return CmsDashboardDto.StatsResponse.builder()
                .totalCourses(courseRepository.count())
                .publishedCourses(courseRepository.countByStatus(CourseStatus.PUBLISHED))
                .draftCourses(courseRepository.countByStatus(CourseStatus.DRAFT))
                .archivedCourses(courseRepository.countByStatus(CourseStatus.ARCHIVED))
                .totalUsers(userRepository.count())
                .totalEnrollments(enrollmentRepository.count())
                .totalCategories(categoryRepository.count())
                .totalSections(sectionRepository.count())
                .totalLectures(lectureRepository.count())
                .totalLectureItems(lectureItemRepository.count())
                .draftItems(lectureItemRepository.countByReviewStatus(ReviewStatus.DRAFT))
                .inReviewItems(lectureItemRepository.countByReviewStatus(ReviewStatus.IN_REVIEW))
                .publishedItems(lectureItemRepository.countByReviewStatus(ReviewStatus.PUBLISHED))
                .recentCourses(recentCourses)
                .build();
    }

    public Page<LectureItemDto.SummaryResponse> getItemsByReviewStatus(ReviewStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return lectureItemRepository.findByReviewStatus(status, pageable)
                .map(lectureItemMapper::toSummaryResponse);
    }
}
