package com.codehaja.domain.lecture.repository;

import com.codehaja.domain.lecture.dto.LectureDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LectureQueryRepositoryImpl implements LectureQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<LectureDto.SummaryResponse> searchLectures(
            Long courseSectionId,
            String keyword,
            Boolean isPublished,
            Boolean isPreview,
            Pageable pageable
    ) {
        StringBuilder fromClause = new StringBuilder("""
            FROM Lecture l
            JOIN l.courseSection cs
            WHERE cs.id = :courseSectionId
        """);

        if (keyword != null && !keyword.isBlank()) {
            fromClause.append("""
                 AND LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """);
        }

        if (isPublished != null) {
            fromClause.append("""
                 AND l.isPublished = :isPublished
            """);
        }

        if (isPreview != null) {
            fromClause.append("""
                 AND l.isPreview = :isPreview
            """);
        }

        String selectQuery = """
            SELECT l.id, cs.id, cs.title, l.title, l.description, l.sortOrder,
                   l.durationMinutes, l.isPreview, l.isPublished, l.lectureType
        """ + fromClause + """
            ORDER BY l.sortOrder ASC, l.id ASC
        """;

        TypedQuery<Object[]> query = em.createQuery(selectQuery, Object[].class);
        query.setParameter("courseSectionId", courseSectionId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (isPublished != null) {
            query.setParameter("isPublished", isPublished);
        }
        if (isPreview != null) {
            query.setParameter("isPreview", isPreview);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object[]> rows = query.getResultList();

        List<LectureDto.SummaryResponse> content = rows.stream().map(row -> {
            LectureDto.SummaryResponse dto = new LectureDto.SummaryResponse();
            dto.setId(((Number) row[0]).longValue());
            dto.setCourseSectionId(((Number) row[1]).longValue());
            dto.setCourseSectionTitle((String) row[2]);
            dto.setTitle((String) row[3]);
            dto.setDescription((String) row[4]);
            dto.setSortOrder(row[5] == null ? 0 : ((Number) row[5]).intValue());
            dto.setDurationMinutes(row[6] == null ? 0 : ((Number) row[6]).intValue());
            dto.setIsPreview((Boolean) row[7]);
            dto.setIsPublished((Boolean) row[8]);
            dto.setLectureType((com.codehaja.domain.lecture.entity.LectureType) row[9]);
            dto.setItemCount(0L);
            return dto;
        }).toList();

        String countQueryString = "SELECT COUNT(l.id) " + fromClause;
        TypedQuery<Long> countQuery = em.createQuery(countQueryString, Long.class);
        countQuery.setParameter("courseSectionId", courseSectionId);

        if (keyword != null && !keyword.isBlank()) {
            countQuery.setParameter("keyword", keyword);
        }
        if (isPublished != null) {
            countQuery.setParameter("isPublished", isPublished);
        }
        if (isPreview != null) {
            countQuery.setParameter("isPreview", isPreview);
        }

        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}