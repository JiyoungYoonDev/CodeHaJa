package com.codehaja.domain.enrollment.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.course.repository.CourseRepository;
import com.codehaja.domain.enrollment.dto.CourseEnrollmentDto;
import com.codehaja.domain.enrollment.entity.CourseEnrollment;
import com.codehaja.domain.enrollment.repository.CourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseEnrollmentService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Transactional
    public CourseEnrollmentDto.StatusResponse enroll(Long courseId, String userEmail) {
        User user = getUser(userEmail);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseGet(() -> {
                    CourseEnrollment e = new CourseEnrollment();
                    e.setUser(user);
                    e.setCourse(course);
                    return enrollmentRepository.save(e);
                });

        CourseEnrollmentDto.StatusResponse res = new CourseEnrollmentDto.StatusResponse();
        res.setCourseId(courseId);
        res.setEnrolled(true);
        res.setEnrolledAt(enrollment.getEnrolledAt());
        return res;
    }

    public CourseEnrollmentDto.StatusResponse getStatus(Long courseId, String userEmail) {
        User user = getUser(userEmail);
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
        CourseEnrollmentDto.StatusResponse res = new CourseEnrollmentDto.StatusResponse();
        res.setCourseId(courseId);
        res.setEnrolled(enrolled);
        return res;
    }

    public Page<CourseEnrollmentDto.EnrollmentListItem> getEnrollmentsByCourse(Long courseId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "enrolledAt"));
        return enrollmentRepository.findByCourseId(courseId, pageable)
                .map(e -> {
                    CourseEnrollmentDto.EnrollmentListItem item = new CourseEnrollmentDto.EnrollmentListItem();
                    item.setId(e.getId());
                    item.setUserId(e.getUser().getId());
                    item.setUserEmail(e.getUser().getEmail());
                    item.setUserName(e.getUser().getName());
                    item.setEnrolledAt(e.getEnrolledAt());
                    return item;
                });
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
    }
}
