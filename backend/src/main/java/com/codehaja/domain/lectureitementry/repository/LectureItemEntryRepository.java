package com.codehaja.domain.lectureitementry.repository;

import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LectureItemEntryRepository extends JpaRepository<LectureItemEntry, Long>, LectureItemEntryQueryRepository {

    @EntityGraph(attributePaths = {
            "lectureItem",
            "lectureItem.lecture",
            "lectureItem.lecture.courseSection",
            "lectureItem.lecture.courseSection.course"
    })
    Optional<LectureItemEntry> findWithHierarchyById(Long id);

    @EntityGraph(attributePaths = {
            "lectureItem",
            "lectureItem.lecture",
            "lectureItem.lecture.courseSection",
            "lectureItem.lecture.courseSection.course"
    })
    List<LectureItemEntry> findAllByLectureItemIdOrderBySortOrderAsc(Long lectureItemId);

    @EntityGraph(attributePaths = {
            "lectureItem",
            "lectureItem.lecture",
            "lectureItem.lecture.courseSection",
            "lectureItem.lecture.courseSection.course"
    })
    List<LectureItemEntry> findAllByLectureItemId(Long lectureItemId);

    Optional<LectureItemEntry> findFirstByLectureItemIdOrderBySortOrderAsc(Long lectureItemId);

    @Query("""
        SELECT COALESCE(MAX(e.sortOrder), 0)
        FROM LectureItemEntry e
        WHERE e.lectureItem.id = :lectureItemId
    """)
    Integer findMaxSortOrderByLectureItemId(Long lectureItemId);
}