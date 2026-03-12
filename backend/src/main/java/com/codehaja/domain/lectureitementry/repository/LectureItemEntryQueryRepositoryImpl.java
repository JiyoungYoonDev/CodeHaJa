package com.codehaja.domain.lectureitementry.repository;

import com.codehaja.domain.lectureitementry.dto.LectureItemEntryDto;
import com.codehaja.domain.lectureitementry.entity.AccessLevel;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntryType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LectureItemEntryQueryRepositoryImpl implements LectureItemEntryQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<LectureItemEntryDto.SummaryResponse> searchEntries(
            Long lectureItemId,
            String keyword,
            LectureItemEntryType entryType,
            AccessLevel accessLevel,
            Boolean isActive,
            Pageable pageable
    ) {
        StringBuilder fromClause = new StringBuilder("""
            FROM LectureItemEntry e
            JOIN e.lectureItem li
            JOIN li.lecture l
            WHERE li.id = :lectureItemId
        """);

        if (keyword != null && !keyword.isBlank()) {
            fromClause.append("""
                 AND LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """);
        }

        if (entryType != null) {
            fromClause.append("""
                 AND e.entryType = :entryType
            """);
        }

        if (accessLevel != null) {
            fromClause.append("""
                 AND e.accessLevel = :accessLevel
            """);
        }

        if (isActive != null) {
            fromClause.append("""
                 AND e.isActive = :isActive
            """);
        }

        String selectQuery = """
            SELECT e.id, li.id, li.title, l.id, l.title, e.title, e.entryType,
                   e.prompt, e.sortOrder, e.points, e.isRequired, e.isActive, e.accessLevel
        """ + fromClause + """
            ORDER BY e.sortOrder ASC, e.id ASC
        """;

        TypedQuery<Object[]> query = em.createQuery(selectQuery, Object[].class);
        query.setParameter("lectureItemId", lectureItemId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (entryType != null) {
            query.setParameter("entryType", entryType);
        }
        if (accessLevel != null) {
            query.setParameter("accessLevel", accessLevel);
        }
        if (isActive != null) {
            query.setParameter("isActive", isActive);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object[]> rows = query.getResultList();

        List<LectureItemEntryDto.SummaryResponse> content = rows.stream().map(row -> {
            LectureItemEntryDto.SummaryResponse dto = new LectureItemEntryDto.SummaryResponse();
            dto.setId(((Number) row[0]).longValue());
            dto.setLectureItemId(((Number) row[1]).longValue());
            dto.setLectureItemTitle((String) row[2]);
            dto.setLectureId(((Number) row[3]).longValue());
            dto.setLectureTitle((String) row[4]);
            dto.setTitle((String) row[5]);
            dto.setEntryType((LectureItemEntryType) row[6]);
            dto.setPrompt((String) row[7]);
            dto.setSortOrder(row[8] == null ? 0 : ((Number) row[8]).intValue());
            dto.setPoints(row[9] == null ? 0 : ((Number) row[9]).intValue());
            dto.setIsRequired((Boolean) row[10]);
            dto.setIsActive((Boolean) row[11]);
            dto.setAccessLevel((AccessLevel) row[12]);
            return dto;
        }).toList();

        String countQueryString = "SELECT COUNT(e.id) " + fromClause;
        TypedQuery<Long> countQuery = em.createQuery(countQueryString, Long.class);
        countQuery.setParameter("lectureItemId", lectureItemId);

        if (keyword != null && !keyword.isBlank()) {
            countQuery.setParameter("keyword", keyword);
        }
        if (entryType != null) {
            countQuery.setParameter("entryType", entryType);
        }
        if (accessLevel != null) {
            countQuery.setParameter("accessLevel", accessLevel);
        }
        if (isActive != null) {
            countQuery.setParameter("isActive", isActive);
        }

        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}