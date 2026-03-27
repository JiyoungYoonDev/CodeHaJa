package com.codehaja.domain.progress.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureItemEntryProgressRepository lectureItemEntryProgressRepository;

    @Transactional
    public LectureProgressDto.Response saveLectureProgress(Long lectureId, LectureProgressDto.SaveRequest request, String userEmail) {
        User user = getUser(userEmail);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        LectureProgress progress = lectureProgressRepository
                .findByUserIdAndLectureId(user.getId(), lectureId)
                .orElseGet(() -> {
                    LectureProgress p = new LectureProgress();
                    p.setUser(user);
                    p.setLecture(lecture);
                    p.setStatus(ProgressStatus.NOT_STARTED);
                    return p;
                });

        progress.setStatus(request.getStatus() == null ? ProgressStatus.IN_PROGRESS : request.getStatus());
        progress.setCurrentItemOrder(request.getCurrentItemOrder());
        progress.setCurrentEntryOrder(request.getCurrentEntryOrder());
        progress.setLastViewedAt(LocalDateTime.now());

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        LectureProgress saved = lectureProgressRepository.save(progress);
        return toResponse(saved);
    }

    public LectureProgressDto.Response getLectureProgress(Long lectureId, String userEmail) {
        User user = getUser(userEmail);
        return lectureProgressRepository
                .findByUserIdAndLectureId(user.getId(), lectureId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public LectureItemEntryProgressDto.Response saveEntryProgress(Long lectureItemEntryId, LectureItemEntryProgressDto.SaveRequest request, String userEmail) {
        User user = getUser(userEmail);

        LectureItemEntry entry = lectureItemEntryRepository.findById(lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        LectureItemEntryProgress progress = lectureItemEntryProgressRepository
                .findByUserIdAndLectureItemEntryId(user.getId(), lectureItemEntryId)
                .orElseGet(() -> {
                    LectureItemEntryProgress p = new LectureItemEntryProgress();
                    p.setUser(user);
                    p.setLectureItemEntry(entry);
                    p.setStatus(ProgressStatus.NOT_STARTED);
                    return p;
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
        return toEntryResponse(saved);
    }

    public LectureItemEntryProgressDto.Response getEntryProgress(Long lectureItemEntryId, String userEmail) {
        User user = getUser(userEmail);

        LectureItemEntryProgress progress = lectureItemEntryProgressRepository
                .findByUserIdAndLectureItemEntryId(user.getId(), lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Entry progress not found."));

        return toEntryResponse(progress);
    }

    public LectureProgressDto.CourseSummary getCourseLectureProgress(Long courseId, String userEmail) {
        User user = getUser(userEmail);
        List<Long> completedIds = lectureProgressRepository
                .findByUserIdAndLecture_CourseSection_CourseId(user.getId(), courseId)
                .stream()
                .filter(p -> p.getStatus() == ProgressStatus.COMPLETED)
                .map(p -> p.getLecture().getId())
                .toList();
        return new LectureProgressDto.CourseSummary(completedIds);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
    }

    private LectureProgressDto.Response toResponse(LectureProgress p) {
        LectureProgressDto.Response r = new LectureProgressDto.Response();
        r.setId(p.getId());
        r.setLectureId(p.getLecture().getId());
        r.setStatus(p.getStatus());
        r.setCurrentItemOrder(p.getCurrentItemOrder());
        r.setCurrentEntryOrder(p.getCurrentEntryOrder());
        return r;
    }

    private LectureItemEntryProgressDto.Response toEntryResponse(LectureItemEntryProgress p) {
        LectureItemEntryProgressDto.Response r = new LectureItemEntryProgressDto.Response();
        r.setId(p.getId());
        r.setLectureItemEntryId(p.getLectureItemEntry().getId());
        r.setStatus(p.getStatus());
        r.setIsCorrect(p.getIsCorrect());
        r.setScore(p.getScore());
        r.setAnswerJson(p.getAnswerJson());
        return r;
    }
}
