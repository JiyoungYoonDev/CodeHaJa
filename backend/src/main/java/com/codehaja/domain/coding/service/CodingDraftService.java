package com.codehaja.domain.coding.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.anonymous.service.AnonymousUserService;
import com.codehaja.domain.coding.dto.CodingDraftDto;
import com.codehaja.domain.coding.entity.CodingDraft;
import com.codehaja.domain.coding.repository.CodingDraftRepository;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import com.codehaja.domain.lectureitementry.repository.LectureItemEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodingDraftService {

    private final AnonymousUserService anonymousUserService;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final CodingDraftRepository codingDraftRepository;

    @Transactional
    public CodingDraftDto.Response saveDraft(Long lectureItemEntryId, CodingDraftDto.SaveRequest request) {
        validateRequest(request);

        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(request.getAnonymousUserKey());

        LectureItemEntry entry = lectureItemEntryRepository.findById(lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        CodingDraft draft = codingDraftRepository
                .findByAnonymousUserIdAndLectureItemEntryId(anonymousUser.getId(), lectureItemEntryId)
                .orElseGet(() -> {
                    CodingDraft newDraft = new CodingDraft();
                    newDraft.setAnonymousUser(anonymousUser);
                    newDraft.setLectureItemEntry(entry);
                    return newDraft;
                });

        draft.setSourceCode(request.getSourceCode());
        draft.setLanguage(request.getLanguage());

        CodingDraft saved = codingDraftRepository.save(draft);

        CodingDraftDto.Response response = new CodingDraftDto.Response();
        response.setId(saved.getId());
        response.setLectureItemEntryId(saved.getLectureItemEntry().getId());
        response.setSourceCode(saved.getSourceCode());
        response.setLanguage(saved.getLanguage());
        return response;
    }

    public CodingDraftDto.Response getDraft(Long lectureItemEntryId, String anonymousUserKey) {
        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(anonymousUserKey);

        CodingDraft draft = codingDraftRepository
                .findByAnonymousUserIdAndLectureItemEntryId(anonymousUser.getId(), lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODING_DRAFT_NOT_FOUND));

        CodingDraftDto.Response response = new CodingDraftDto.Response();
        response.setId(draft.getId());
        response.setLectureItemEntryId(draft.getLectureItemEntry().getId());
        response.setSourceCode(draft.getSourceCode());
        response.setLanguage(draft.getLanguage());
        return response;
    }

    private void validateRequest(CodingDraftDto.SaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getAnonymousUserKey() == null || request.getAnonymousUserKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }
    }
}