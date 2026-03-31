package com.codehaja.domain.lectureitem.repository;

import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LectureItemQueryRepositoryImpl implements LectureItemQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<LectureItemDto.SummaryResponse> searchLectureItems(
            Long lectureId,
            String keyword,
            LectureItemType itemType,
            ReviewStatus reviewStatus,
            Pageable pageable
    ) {
        StringBuilder fromClause = new StringBuilder("""
            FROM LectureItem li
            JOIN li.lecture l
            WHERE l.id = :lectureId
        """);

        if (keyword != null && !keyword.isBlank()) {
            fromClause.append("""
                 AND LOWER(li.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """);
        }

        if (itemType != null) {
            fromClause.append("""
                 AND li.itemType = :itemType
            """);
        }

        if (reviewStatus != null) {
            fromClause.append("""
                 AND li.reviewStatus = :reviewStatus
            """);
        }

        String selectQuery = """
            SELECT li.id, l.id, l.title, li.title, li.itemType, li.description,
                   li.sortOrder, li.points, li.isRequired
        """ + fromClause + """
            ORDER BY li.sortOrder ASC, li.id ASC
        """;

        TypedQuery<Object[]> query = em.createQuery(selectQuery, Object[].class);
        query.setParameter("lectureId", lectureId);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (itemType != null) {
            query.setParameter("itemType", itemType);
        }
        if (reviewStatus != null) {
            query.setParameter("reviewStatus", reviewStatus);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object[]> rows = query.getResultList();

        List<LectureItemDto.SummaryResponse> content = rows.stream().map(row -> {
            LectureItemDto.SummaryResponse dto = new LectureItemDto.SummaryResponse();
            dto.setId(((Number) row[0]).longValue());
            dto.setLectureId(((Number) row[1]).longValue());
            dto.setLectureTitle((String) row[2]);
            dto.setTitle((String) row[3]);
            dto.setItemType((LectureItemType) row[4]);
            dto.setDescription((String) row[5]);
            dto.setSortOrder(row[6] == null ? 0 : ((Number) row[6]).intValue());
            dto.setPoints(row[7] == null ? 0 : ((Number) row[7]).intValue());
            dto.setIsRequired((Boolean) row[8]);
            dto.setEntryCount(0L);
            return dto;
        }).toList();

        String countQueryString = "SELECT COUNT(li.id) " + fromClause;
        TypedQuery<Long> countQuery = em.createQuery(countQueryString, Long.class);
        countQuery.setParameter("lectureId", lectureId);

        if (keyword != null && !keyword.isBlank()) {
            countQuery.setParameter("keyword", keyword);
        }
        if (itemType != null) {
            countQuery.setParameter("itemType", itemType);
        }
        if (reviewStatus != null) {
            countQuery.setParameter("reviewStatus", reviewStatus);
        }

        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}