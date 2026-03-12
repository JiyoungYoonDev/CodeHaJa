package com.codehaja.domain.section.repository;

import com.codehaja.domain.section.entity.CourseSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long>, CourseSectionQueryRepository {

    @EntityGraph(attributePaths = {"course"})
    Optional<CourseSection> findWithCourseById(Long id);

    @EntityGraph(attributePaths = {"course"})
    List<CourseSection> findAllByCourseIdOrderBySortOrderAsc(Long courseId);

    @EntityGraph(attributePaths = {"course"})
    List<CourseSection> findAllByCourseId(Long courseId);

    @Query("""
        SELECT COALESCE(MAX(cs.sortOrder), 0)
        FROM CourseSection cs
        WHERE cs.course.id = :courseId
    """)
    Integer findMaxSortOrderByCourseId(Long courseId);
}