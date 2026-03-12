package com.codehaja.domain.lectureitementry.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.lectureitementry.dto.LectureItemEntryDto;
import com.codehaja.domain.lectureitementry.entity.AccessLevel;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntryType;
import com.codehaja.domain.lectureitementry.mapper.LectureItemEntryMapper;
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
public class LectureItemEntryService {

    private final LectureItemRepository lectureItemRepository;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final LectureItemEntryMapper lectureItemEntryMapper;

    @Transactional
    public LectureItemEntryDto.DetailResponse createEntry(Long lectureItemId, LectureItemEntryDto.CreateRequest request) {
        validateCreateRequest(request);

        LectureItem lectureItem = getLectureItemOrThrow(lectureItemId);

        LectureItemEntry entry = lectureItemEntryMapper.toEntity(request);
        entry.setLectureItem(lectureItem);

        applyDefaultsOnCreate(lectureItemId, entry);

        LectureItemEntry saved = lectureItemEntryRepository.save(entry);
        return lectureItemEntryMapper.toDetailResponse(saved);
    }

    public Page<LectureItemEntryDto.SummaryResponse> getEntries(
            Long lectureItemId,
            int page,
            int size,
            String keyword,
            LectureItemEntryType entryType,
            AccessLevel accessLevel,
            Boolean isActive
    ) {
        getLectureItemOrThrow(lectureItemId);

        int safePage = Math.max(page, 0);
        int safeSize = normalizePageSize(size);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        return lectureItemEntryRepository.searchEntries(
                lectureItemId, keyword, entryType, accessLevel, isActive, pageable
        );
    }

    public LectureItemEntryDto.DetailResponse getEntry(Long entryId) {
        LectureItemEntry entry = lectureItemEntryRepository.findWithHierarchyById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        return lectureItemEntryMapper.toDetailResponse(entry);
    }

    @Transactional
    public LectureItemEntryDto.DetailResponse updateEntry(Long entryId, LectureItemEntryDto.UpdateRequest request) {
        validateUpdateRequest(request);

        LectureItemEntry entry = lectureItemEntryRepository.findWithHierarchyById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        lectureItemEntryMapper.updateEntityFromDto(request, entry);
        applyDefaultsOnUpdate(entry);

        return lectureItemEntryMapper.toDetailResponse(entry);
    }

    @Transactional
    public void deleteEntry(Long entryId) {
        LectureItemEntry entry = lectureItemEntryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        lectureItemEntryRepository.delete(entry);
    }

    @Transactional
    public List<LectureItemEntryDto.SummaryResponse> reorderEntries(
            Long lectureItemId,
            List<LectureItemEntryDto.ReorderRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTRY_REORDER_INVALID, "Reorder request is empty.");
        }

        getLectureItemOrThrow(lectureItemId);

        List<LectureItemEntry> entries = lectureItemEntryRepository.findAllByLectureItemId(lectureItemId);
        if (entries.isEmpty()) {
            throw new BusinessException(ErrorCode.ENTRY_NOT_FOUND, "No entries found for this lecture item.");
        }

        Map<Long, LectureItemEntry> entryMap = entries.stream()
                .collect(Collectors.toMap(LectureItemEntry::getId, Function.identity()));

        Set<Long> requestIds = new HashSet<>();
        for (LectureItemEntryDto.ReorderRequest request : requests) {
            if (request.getId() == null || request.getSortOrder() == null) {
                throw new BusinessException(ErrorCode.ENTRY_REORDER_INVALID, "Entry id and sortOrder are required.");
            }

            if (!entryMap.containsKey(request.getId())) {
                throw new BusinessException(ErrorCode.ENTRY_REORDER_INVALID, "Entry does not belong to this lecture item.");
            }

            if (!requestIds.add(request.getId())) {
                throw new BusinessException(ErrorCode.ENTRY_REORDER_INVALID, "Duplicate entry id in reorder request.");
            }
        }

        for (LectureItemEntryDto.ReorderRequest request : requests) {
            LectureItemEntry entry = entryMap.get(request.getId());
            entry.setSortOrder(request.getSortOrder());
        }

        List<LectureItemEntry> updated = lectureItemEntryRepository.findAllByLectureItemIdOrderBySortOrderAsc(lectureItemId);

        return updated.stream()
                .map(lectureItemEntryMapper::toSummaryResponse)
                .toList();
    }

    private LectureItem getLectureItemOrThrow(Long lectureItemId) {
        return lectureItemRepository.findWithLectureHierarchyById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));
    }

    private void validateCreateRequest(LectureItemEntryDto.CreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Entry title is required.");
        }

        if (request.getEntryType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Entry type is required.");
        }
    }

    private void validateUpdateRequest(LectureItemEntryDto.UpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }

        if (isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Entry title is required.");
        }

        if (request.getEntryType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Entry type is required.");
        }
    }

    private void applyDefaultsOnCreate(Long lectureItemId, LectureItemEntry entry) {
        if (entry.getPoints() == null) {
            entry.setPoints(0);
        }
        if (entry.getIsRequired() == null) {
            entry.setIsRequired(true);
        }
        if (entry.getIsActive() == null) {
            entry.setIsActive(true);
        }
        if (entry.getAccessLevel() == null) {
            entry.setAccessLevel(AccessLevel.FREE);
        }
        if (entry.getSortOrder() == null || entry.getSortOrder() <= 0) {
            Integer maxSortOrder = lectureItemEntryRepository.findMaxSortOrderByLectureItemId(lectureItemId);
            entry.setSortOrder((maxSortOrder == null ? 0 : maxSortOrder) + 1);
        }
    }

    private void applyDefaultsOnUpdate(LectureItemEntry entry) {
        if (entry.getPoints() == null) {
            entry.setPoints(0);
        }
        if (entry.getIsRequired() == null) {
            entry.setIsRequired(true);
        }
        if (entry.getIsActive() == null) {
            entry.setIsActive(true);
        }
        if (entry.getAccessLevel() == null) {
            entry.setAccessLevel(AccessLevel.FREE);
        }
        if (entry.getSortOrder() == null || entry.getSortOrder() <= 0) {
            entry.setSortOrder(1);
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