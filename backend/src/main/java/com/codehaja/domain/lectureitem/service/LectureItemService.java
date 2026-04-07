package com.codehaja.domain.lectureitem.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.lecture.entity.Lecture;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import com.codehaja.domain.lectureitem.mapper.LectureItemMapper;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.lectureitementry.repository.LectureItemEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureItemService {

    private final LectureRepository lectureRepository;
    private final LectureItemRepository lectureItemRepository;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final LectureItemMapper lectureItemMapper;

    @Transactional
    public LectureItemDto.DetailResponse createLectureItem(Long lectureId, LectureItemDto.CreateRequest request) {
        validateCreateRequest(request);

        Lecture lecture = getLectureOrThrow(lectureId);

        LectureItem lectureItem = lectureItemMapper.toEntity(request);
        lectureItem.setLecture(lecture);

        applyDefaultsOnCreate(lectureId, lectureItem);

        LectureItem saved = lectureItemRepository.save(lectureItem);
        return lectureItemMapper.toDetailResponse(saved);
    }

    public Page<LectureItemDto.SummaryResponse> getLectureItems(
            Long lectureId,
            int page,
            int size,
            String keyword,
            LectureItemType itemType,
            ReviewStatus reviewStatus
    ) {
        getLectureOrThrow(lectureId);

        int safePage = Math.max(page, 0);
        int safeSize = normalizePageSize(size);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<LectureItemDto.SummaryResponse> result =
                lectureItemRepository.searchLectureItems(lectureId, keyword, itemType, reviewStatus, pageable);

        result.getContent().forEach(item ->
                lectureItemEntryRepository.findFirstByLectureItemIdOrderBySortOrderAsc(item.getId())
                        .ifPresent(entry -> item.setFirstEntryId(entry.getId()))
        );

        return result;
    }

    public LectureItemDto.DetailResponse getLectureItem(Long lectureItemId) {
        LectureItem lectureItem = lectureItemRepository.findWithLectureHierarchyById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        return lectureItemMapper.toDetailResponse(lectureItem);
    }

    @Transactional
    public LectureItemDto.DetailResponse updateLectureItem(Long lectureItemId, LectureItemDto.UpdateRequest request) {
        validateUpdateRequest(request);

        LectureItem lectureItem = lectureItemRepository.findWithLectureHierarchyById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        lectureItemMapper.updateEntityFromDto(request, lectureItem);
        applyDefaultsOnUpdate(lectureItem);

        return lectureItemMapper.toDetailResponse(lectureItem);
    }

    @Transactional
    public LectureItemDto.DetailResponse updateReviewStatus(Long lectureItemId, LectureItemDto.ReviewStatusRequest request) {
        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        if (request.getReviewStatus() != null) {
            lectureItem.setReviewStatus(request.getReviewStatus());
        }

        return lectureItemMapper.toDetailResponse(lectureItem);
    }

    @Transactional
    public void deleteLectureItem(Long lectureItemId) {
        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        lectureItemRepository.delete(lectureItem);
    }

    @Transactional
    public List<LectureItemDto.SummaryResponse> reorderLectureItems(
            Long lectureId,
            List<LectureItemDto.ReorderRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.LECTURE_ITEM_REORDER_INVALID, "Reorder request is empty.");
        }

        getLectureOrThrow(lectureId);

        List<LectureItem> lectureItems = lectureItemRepository.findAllByLectureId(lectureId);
        if (lectureItems.isEmpty()) {
            throw new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND, "No lecture items found for this lecture.");
        }

        Map<Long, LectureItem> lectureItemMap = lectureItems.stream()
                .collect(Collectors.toMap(LectureItem::getId, Function.identity()));

        Set<Long> requestIds = new HashSet<>();
        for (LectureItemDto.ReorderRequest request : requests) {
            if (request.getId() == null || request.getSortOrder() == null) {
                throw new BusinessException(ErrorCode.LECTURE_ITEM_REORDER_INVALID, "Lecture item id and sortOrder are required.");
            }

            if (!lectureItemMap.containsKey(request.getId())) {
                throw new BusinessException(ErrorCode.LECTURE_ITEM_REORDER_INVALID, "Lecture item does not belong to this lecture.");
            }

            if (!requestIds.add(request.getId())) {
                throw new BusinessException(ErrorCode.LECTURE_ITEM_REORDER_INVALID, "Duplicate lecture item id in reorder request.");
            }
        }

        for (LectureItemDto.ReorderRequest request : requests) {
            LectureItem lectureItem = lectureItemMap.get(request.getId());
            lectureItem.setSortOrder(request.getSortOrder());
        }

        List<LectureItem> updated = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lectureId);

        return updated.stream()
                .map(lectureItemMapper::toSummaryResponse)
                .toList();
    }

    private Lecture getLectureOrThrow(Long lectureId) {
        return lectureRepository.findWithSectionAndCourseById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));
    }

    private void validateCreateRequest(LectureItemDto.CreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture item title is required.");
        }

        if (request.getItemType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture item type is required.");
        }
    }

    private void validateUpdateRequest(LectureItemDto.UpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture item title is required.");
        }

        if (request.getItemType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Lecture item type is required.");
        }
    }

    private void applyDefaultsOnCreate(Long lectureId, LectureItem lectureItem) {
        if (lectureItem.getReviewStatus() == null) {
            lectureItem.setReviewStatus(ReviewStatus.DRAFT);
        }
        if (lectureItem.getPoints() == null) {
            lectureItem.setPoints(0);
        }
        if (lectureItem.getIsRequired() == null) {
            lectureItem.setIsRequired(true);
        }
        if (lectureItem.getSortOrder() == null || lectureItem.getSortOrder() <= 0) {
            Integer maxSortOrder = lectureItemRepository.findMaxSortOrderByLectureId(lectureId);
            lectureItem.setSortOrder((maxSortOrder == null ? 0 : maxSortOrder) + 1);
        }
    }

    private void applyDefaultsOnUpdate(LectureItem lectureItem) {
        if (lectureItem.getPoints() == null) {
            lectureItem.setPoints(0);
        }
        if (lectureItem.getIsRequired() == null) {
            lectureItem.setIsRequired(true);
        }
        if (lectureItem.getSortOrder() == null || lectureItem.getSortOrder() <= 0) {
            lectureItem.setSortOrder(1);
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}