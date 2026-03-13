package com.codehaja.domain.section.repository;

import com.codehaja.domain.section.dto.CourseSectionDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CourseSectionQueryRepositoryImpl implements CourseSectionQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<CourseSectionDto.SummaryResponse> searchSections(Long courseId, String keyword, Pageable pageable) {
        StringBuilder fromClause = new StringBuilder("""
            FROM CourseSection cs
            JOIN cs.course c
            WHERE c.id = :courseId
        """);

        if (keyword != null && !keyword.isBlank()) {
            fromClause.append("""
                 AND LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """);
        }

        String selectQuery = """
            SELECT cs.id, c.id, c.title, cs.title, cs.description, cs.hours, cs.points, cs.sortOrder
        """ + fromClause + """
            ORDER BY cs.sortOrder ASC, cs.id ASC
        """;

        TypedQuery<Object[]> query = em.createQuery(selectQuery, Object[].class);
        query.setParameter("courseId", courseId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object[]> rows = query.getResultList();

        List<CourseSectionDto.SummaryResponse> content = rows.stream().map(row -> {
            CourseSectionDto.SummaryResponse dto = new CourseSectionDto.SummaryResponse();
            dto.setId(((Number) row[0]).longValue());
            dto.setCourseId(((Number) row[1]).longValue());
            dto.setCourseTitle((String) row[2]);
            dto.setTitle((String) row[3]);
            dto.setDescription((String) row[4]);
            dto.setHours(row[5] == null ? 0 : ((Number) row[5]).intValue());
            dto.setPoints(row[6] == null ? 0 : ((Number) row[6]).intValue());
            dto.setSortOrder(row[7] == null ? 0 : ((Number) row[7]).intValue());
            dto.setLectureCount(0L);
            return dto;
        }).toList();

        String countQueryString = "SELECT COUNT(cs.id) " + fromClause;
        TypedQuery<Long> countQuery = em.createQuery(countQueryString, Long.class);
        countQuery.setParameter("courseId", courseId);

        if (keyword != null && !keyword.isBlank()) {
            countQuery.setParameter("keyword", keyword);
        }

        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}