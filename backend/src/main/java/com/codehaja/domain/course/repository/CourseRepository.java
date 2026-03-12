package com.codehaja.domain.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.codehaja.domain.category.entity.CourseCategory;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;


public interface CourseRepository extends JpaRepository<Course, Long> {
    
    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByCategoryOrderByCreatedAtDesc(CourseCategory category);

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByDifficultyOrderByCreatedAtDesc(Difficulty difficulty);

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByStatusOrderByCreatedAtDesc(CourseStatus status);

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByCategoryAndDifficultyOrderByCreatedAtDesc(
            CourseCategory category,
            Difficulty difficulty
    );

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByCategoryAndStatusOrderByCreatedAtDesc(
            CourseCategory category,
            CourseStatus status
    );

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByDifficultyAndStatusOrderByCreatedAtDesc(
            Difficulty difficulty,
            CourseStatus status
    );

    @EntityGraph(attributePaths = {"category"})
    List<Course> findAllByCategoryAndDifficultyAndStatusOrderByCreatedAtDesc(
            CourseCategory category,
            Difficulty difficulty,
            CourseStatus status
    );
}
