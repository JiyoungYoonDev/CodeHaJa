package com.codehaja.domain.lectureitem.repository;

import com.codehaja.domain.lectureitem.entity.LectureItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LectureItemRepository extends JpaRepository<LectureItem, Long>, LectureItemQueryRepository {

    @EntityGraph(attributePaths = {
            "lecture",
            "lecture.courseSection",
            "lecture.courseSection.course"
    })
    Optional<LectureItem> findWithLectureHierarchyById(Long id);

    @EntityGraph(attributePaths = {
            "lecture",
            "lecture.courseSection",
            "lecture.courseSection.course"
    })
    List<LectureItem> findAllByLectureIdOrderBySortOrderAsc(Long lectureId);

    @EntityGraph(attributePaths = {
            "lecture",
            "lecture.courseSection",
            "lecture.courseSection.course"
    })
    List<LectureItem> findAllByLectureId(Long lectureId);

    Optional<LectureItem> findFirstByLectureIdOrderBySortOrderAsc(Long lectureId);

    @Query("""
        SELECT COALESCE(MAX(li.sortOrder), 0)
        FROM LectureItem li
        WHERE li.lecture.id = :lectureId
    """)
    Integer findMaxSortOrderByLectureId(Long lectureId);
}