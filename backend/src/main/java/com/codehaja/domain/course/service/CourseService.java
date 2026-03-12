package com.codehaja.domain.course.service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final CourseMapper courseMapper;

    @Transactional
    public CourseDto.Response createCourse(CourseDto.CreateRequest request) {
        validateCreateRequest(request);

        CourseCategory category = getCategoryOrThrow(request.getCategoryId());

        Course course = courseMapper.toEntity(request);
        course.setCategory(category);
        applyDefaults(course);

        Course saved = courseRepository.save(course);
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

    public CourseDto.Response getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course not found."));

        return courseMapper.toResponse(course);
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
            course.setRating(0);
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
