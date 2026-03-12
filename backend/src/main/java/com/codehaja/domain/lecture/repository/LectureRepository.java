package com.codehaja.domain.lecture.repository;

import com.codehaja.domain.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long>, LectureQueryRepository {

    @EntityGraph(attributePaths = {"courseSection", "courseSection.course"})
    Optional<Lecture> findWithSectionAndCourseById(Long id);

    @EntityGraph(attributePaths = {"courseSection", "courseSection.course"})
    List<Lecture> findAllByCourseSectionIdOrderBySortOrderAsc(Long courseSectionId);

    @EntityGraph(attributePaths = {"courseSection", "courseSection.course"})
    List<Lecture> findAllByCourseSectionId(Long courseSectionId);

    @Query("""
        SELECT COALESCE(MAX(l.sortOrder), 0)
        FROM Lecture l
        WHERE l.courseSection.id = :courseSectionId
    """)
    Integer findMaxSortOrderByCourseSectionId(Long courseSectionId);
}