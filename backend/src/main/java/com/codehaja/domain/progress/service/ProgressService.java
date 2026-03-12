package com.codehaja.domain.progress.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.anonymous.service.AnonymousUserService;
import com.codehaja.domain.lecture.entity.Lecture;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import com.codehaja.domain.lectureitementry.repository.LectureItemEntryRepository;
import com.codehaja.domain.progress.dto.LectureItemEntryProgressDto;
import com.codehaja.domain.progress.dto.LectureProgressDto;
import com.codehaja.domain.progress.entity.LectureItemEntryProgress;
import com.codehaja.domain.progress.entity.LectureProgress;
import com.codehaja.domain.progress.entity.ProgressStatus;
import com.codehaja.domain.progress.repository.LectureItemEntryProgressRepository;
import com.codehaja.domain.progress.repository.LectureProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private final AnonymousUserService anonymousUserService;
    private final LectureRepository lectureRepository;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureItemEntryProgressRepository lectureItemEntryProgressRepository;

    @Transactional
    public LectureProgressDto.Response saveLectureProgress(Long lectureId, LectureProgressDto.SaveRequest request) {
        validateLectureProgressRequest(request);

        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(request.getAnonymousUserKey());

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        LectureProgress progress = lectureProgressRepository
                .findByAnonymousUserIdAndLectureId(anonymousUser.getId(), lectureId)
                .orElseGet(() -> {
                    LectureProgress newProgress = new LectureProgress();
                    newProgress.setAnonymousUser(anonymousUser);
                    newProgress.setLecture(lecture);
                    newProgress.setStatus(ProgressStatus.NOT_STARTED);
                    return newProgress;
                });

        progress.setStatus(request.getStatus() == null ? ProgressStatus.IN_PROGRESS : request.getStatus());
        progress.setCurrentItemOrder(request.getCurrentItemOrder());
        progress.setCurrentEntryOrder(request.getCurrentEntryOrder());
        progress.setLastViewedAt(LocalDateTime.now());

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        LectureProgress saved = lectureProgressRepository.save(progress);

        LectureProgressDto.Response response = new LectureProgressDto.Response();
        response.setId(saved.getId());
        response.setLectureId(saved.getLecture().getId());
        response.setStatus(saved.getStatus());
        response.setCurrentItemOrder(saved.getCurrentItemOrder());
        response.setCurrentEntryOrder(saved.getCurrentEntryOrder());
        return response;
    }

    public LectureProgressDto.Response getLectureProgress(Long lectureId, String anonymousUserKey) {
        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(anonymousUserKey);

        LectureProgress progress = lectureProgressRepository
                .findByAnonymousUserIdAndLectureId(anonymousUser.getId(), lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Lecture progress not found."));

        LectureProgressDto.Response response = new LectureProgressDto.Response();
        response.setId(progress.getId());
        response.setLectureId(progress.getLecture().getId());
        response.setStatus(progress.getStatus());
        response.setCurrentItemOrder(progress.getCurrentItemOrder());
        response.setCurrentEntryOrder(progress.getCurrentEntryOrder());
        return response;
    }

    @Transactional
    public LectureItemEntryProgressDto.Response saveEntryProgress(
            Long lectureItemEntryId,
            LectureItemEntryProgressDto.SaveRequest request
    ) {
        validateEntryProgressRequest(request);

        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(request.getAnonymousUserKey());

        LectureItemEntry entry = lectureItemEntryRepository.findById(lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        LectureItemEntryProgress progress = lectureItemEntryProgressRepository
                .findByAnonymousUserIdAndLectureItemEntryId(anonymousUser.getId(), lectureItemEntryId)
                .orElseGet(() -> {
                    LectureItemEntryProgress newProgress = new LectureItemEntryProgress();
                    newProgress.setAnonymousUser(anonymousUser);
                    newProgress.setLectureItemEntry(entry);
                    newProgress.setStatus(ProgressStatus.NOT_STARTED);
                    return newProgress;
                });

        progress.setStatus(request.getStatus() == null ? ProgressStatus.IN_PROGRESS : request.getStatus());
        progress.setIsCorrect(request.getIsCorrect());
        progress.setScore(request.getScore());
        progress.setAnswerJson(request.getAnswerJson());
        progress.setLastAttemptedAt(LocalDateTime.now());

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        LectureItemEntryProgress saved = lectureItemEntryProgressRepository.save(progress);

        LectureItemEntryProgressDto.Response response = new LectureItemEntryProgressDto.Response();
        response.setId(saved.getId());
        response.setLectureItemEntryId(saved.getLectureItemEntry().getId());
        response.setStatus(saved.getStatus());
        response.setIsCorrect(saved.getIsCorrect());
        response.setScore(saved.getScore());
        response.setAnswerJson(saved.getAnswerJson());
        return response;
    }

    public LectureItemEntryProgressDto.Response getEntryProgress(Long lectureItemEntryId, String anonymousUserKey) {
        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(anonymousUserKey);

        LectureItemEntryProgress progress = lectureItemEntryProgressRepository
                .findByAnonymousUserIdAndLectureItemEntryId(anonymousUser.getId(), lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Entry progress not found."));

        LectureItemEntryProgressDto.Response response = new LectureItemEntryProgressDto.Response();
        response.setId(progress.getId());
        response.setLectureItemEntryId(progress.getLectureItemEntry().getId());
        response.setStatus(progress.getStatus());
        response.setIsCorrect(progress.getIsCorrect());
        response.setScore(progress.getScore());
        response.setAnswerJson(progress.getAnswerJson());
        return response;
    }

    private void validateLectureProgressRequest(LectureProgressDto.SaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getAnonymousUserKey() == null || request.getAnonymousUserKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }
    }

    private void validateEntryProgressRequest(LectureItemEntryProgressDto.SaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getAnonymousUserKey() == null || request.getAnonymousUserKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }
    }
}