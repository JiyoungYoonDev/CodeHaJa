package com.codehaja.domain.course.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.category.entity.CourseCategory;
import com.codehaja.domain.category.repository.CourseCategoryRepository;
import com.codehaja.domain.course.dto.CourseDto;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;
import com.codehaja.domain.course.mapper.CourseMapper;
import com.codehaja.domain.course.repository.CourseRepository;
import com.codehaja.domain.lecture.dto.LectureDto;
import com.codehaja.domain.lecture.mapper.LectureMapper;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.section.dto.CourseSectionDto;
import com.codehaja.domain.section.entity.CourseSection;
import com.codehaja.domain.section.mapper.CourseSectionMapper;
import com.codehaja.domain.section.repository.CourseSectionRepository;
import com.codehaja.domain.enrollment.repository.CourseEnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LectureRepository lectureRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseMapper courseMapper;
    private final CourseSectionMapper courseSectionMapper;
    private final LectureMapper lectureMapper;

    @Transactional
    public CourseDto.Response createCourse(CourseDto.CreateRequest request) {
        validateCreateRequest(request);

        CourseCategory category = getCategoryOrThrow(request.getCategoryId());

        Course course = courseMapper.toEntity(request);
        course.setCategory(category);
        applyDefaults(course);

        Course saved = courseRepository.save(course);
        // createSectionsIfProvided(saved, request.getSections());
        return courseMapper.toResponse(saved);
    }

    public List<CourseDto.Response> getCourses(Long categoryId, Difficulty difficulty, CourseStatus status) {
        List<Course> courses;

        if (categoryId != null && difficulty != null && status != null) {
            CourseCategory category = getCategoryOrThrow(categoryId);
            courses = courseRepository.findAllByCategoryAndDifficultyAndStatusOrderByCreatedAtDesc(
                    category, difficulty, status
            );
        } else if (categoryId != null && difficulty != null) {
            CourseCategory category = getCategoryOrThrow(categoryId);
            courses = courseRepository.findAllByCategoryAndDifficultyOrderByCreatedAtDesc(
                    category, difficulty
            );
        } else if (categoryId != null && status != null) {
            CourseCategory category = getCategoryOrThrow(categoryId);
            courses = courseRepository.findAllByCategoryAndStatusOrderByCreatedAtDesc(
                    category, status
            );
        } else if (difficulty != null && status != null) {
            courses = courseRepository.findAllByDifficultyAndStatusOrderByCreatedAtDesc(
                    difficulty, status
            );
        } else if (categoryId != null) {
            CourseCategory category = getCategoryOrThrow(categoryId);
            courses = courseRepository.findAllByCategoryOrderByCreatedAtDesc(category);
        } else if (difficulty != null) {
            courses = courseRepository.findAllByDifficultyOrderByCreatedAtDesc(difficulty);
        } else if (status != null) {
            courses = courseRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            courses = courseRepository.findAllByOrderByCreatedAtDesc();
        }

        return courses.stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public CourseDto.DetailResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        CourseDto.DetailResponse response = courseMapper.toDetailResponse(course);

        List<CourseSectionDto.SummaryResponse> sections =
                courseSectionRepository.findAllByCourseIdOrderBySortOrderAsc(courseId)
                        .stream()
                        .map(section -> {
                            CourseSectionDto.SummaryResponse sectionResponse = courseSectionMapper.toSummaryResponse(section);
                            List<LectureDto.SummaryResponse> lectures =
                                    lectureRepository.findAllByCourseSectionIdOrderBySortOrderAsc(section.getId())
                                            .stream()
                                            .map(lecture -> {
                                                LectureDto.SummaryResponse lr = lectureMapper.toSummaryResponse(lecture);
                                                lectureItemRepository.findFirstByLectureIdOrderBySortOrderAsc(lecture.getId())
                                                        .ifPresent(item -> lr.setFirstItemId(item.getId()));
                                                lr.setItemCount(lectureItemRepository.countByLectureId(lecture.getId()));
                                                return lr;
                                            })
                                            .toList();
                            sectionResponse.setLectures(lectures);
                            return sectionResponse;
                        })
                        .toList();

        response.setSections(sections);
        response.setTotalSections(sections.size());
        response.setDetailedCurriculum(course.getDetailedCurriculum() != null ? course.getDetailedCurriculum().toString() : null);
        return response;
    }

    private void replaceSections(Course course, List<CourseSectionDto.UpdateRequest> sections) {
        courseSectionRepository.deleteAllByCourseId(course.getId());

        if (sections == null || sections.isEmpty()) {
            return;
        }

        List<CourseSection> entities = new ArrayList<>();
        int fallbackOrder = 1;

        for (CourseSectionDto.UpdateRequest request : sections) {
            if (request == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Section payload is required.");
            }

            if (isBlank(request.getTitle())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Section title is required.");
            }

            Integer sortOrder = request.getSortOrder();
            if (sortOrder != null && sortOrder < 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Section sort order must be positive.");
            }

            CourseSection section = new CourseSection();
            section.setCourse(course);
            section.setTitle(request.getTitle());
            section.setDescription(request.getDescription());
            section.setHours(request.getHours() != null ? request.getHours() : 0);
            section.setPoints(request.getPoints() != null ? request.getPoints() : 0);
            section.setSortOrder(sortOrder != null ? sortOrder : fallbackOrder);

            entities.add(section);

            fallbackOrder++;
        }

        courseSectionRepository.saveAll(entities);
    }
    @Transactional
    public CourseDto.Response updateCourse(Long courseId, CourseDto.UpdateRequest request) {
        validateUpdateRequest(request);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course not found."));

        CourseCategory category = getCategoryOrThrow(request.getCategoryId());

        courseMapper.updateEntityFromDto(request, course);
        course.setCategory(category);

        applyDefaults(course);
        replaceSections(course, request.getSections());

        return courseMapper.toResponse(course);
    }

    @Transactional
    public CourseDto.Response updateCourseStatus(Long courseId, CourseStatus status) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course not found."));

        if (status == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Course status is required.");
        }

        course.setStatus(status);
        return courseMapper.toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course not found."));

        courseEnrollmentRepository.deleteAllByCourseId(courseId);
        courseSectionRepository.deleteAllByCourseId(courseId);
        courseRepository.delete(course);
    }

    private CourseCategory getCategoryOrThrow(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Category id is required.");
        }

        return courseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateCreateRequest(CourseDto.CreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Course title is required.");
        }

        if (request.getDifficulty() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Difficulty is required.");
        }

        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Course status is required.");
        }
    }

    private void validateUpdateRequest(CourseDto.UpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Course title is required.");
        }

        if (request.getDifficulty() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Difficulty is required.");
        }

        if (request.getStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Course status is required.");
        }
    }

    private void applyDefaults(Course course) {
        if (course.getRating() == null) {
            course.setRating(0f);
        }
        if (course.getProjectsCount() == null) {
            course.setProjectsCount(0);
        }
        if (course.getHours() == null) {
            course.setHours(0);
        }
        if (course.getLearnersCount() == null) {
            course.setLearnersCount(0);
        }
        if (course.getBadgeType() == null) {
            course.setBadgeType("New");
        }
        if (course.getProvider() == null || course.getProvider().isBlank()) {
            course.setProvider("Codehaja");
        }
        if (course.getImageUrl() == null) {
            course.setImageUrl("");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
