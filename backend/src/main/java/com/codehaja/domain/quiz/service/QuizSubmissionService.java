package com.codehaja.domain.quiz.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.gamification.service.HeartService;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.quiz.dto.QuizSubmissionDto;
import com.codehaja.domain.quiz.entity.QuizSubmission;
import com.codehaja.domain.quiz.repository.QuizSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSubmissionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final HeartService heartService;

    @Transactional
    public QuizSubmissionDto.Response submit(Long lectureItemId, QuizSubmissionDto.CreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        // Require heart before quiz attempt
        heartService.requireHeart(user);

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        QuizSubmission submission = new QuizSubmission();
        submission.setUser(user);
        submission.setLectureItem(lectureItem);
        submission.setTotalPoints(request.getTotalPoints());
        submission.setEarnedPoints(request.getEarnedPoints());

        if (request.getAnswers() != null && !request.getAnswers().isBlank()) {
            try {
                submission.setAnswers(OBJECT_MAPPER.readTree(request.getAnswers()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid answers JSON.");
            }
        }

        // Deduct heart if not perfect score
        boolean perfect = request.getTotalPoints() > 0
                && request.getEarnedPoints() >= request.getTotalPoints();
        if (!perfect) {
            heartService.deductHeart(user);
        }

        return toResponse(quizSubmissionRepository.save(submission), user);
    }

    @Transactional
    public QuizSubmissionDto.Response saveProgress(Long lectureItemId, QuizSubmissionDto.CreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        // Upsert: find existing row or create new
        QuizSubmission submission = quizSubmissionRepository
                .findByLectureItemIdAndUserId(lectureItemId, user.getId())
                .orElseGet(() -> {
                    QuizSubmission s = new QuizSubmission();
                    s.setUser(user);
                    s.setLectureItem(lectureItem);
                    return s;
                });

        submission.setTotalPoints(request.getTotalPoints());
        submission.setEarnedPoints(request.getEarnedPoints());

        if (request.getAnswers() != null && !request.getAnswers().isBlank()) {
            try {
                submission.setAnswers(OBJECT_MAPPER.readTree(request.getAnswers()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid answers JSON.");
            }
        }

        return toResponse(quizSubmissionRepository.save(submission));
    }

    public QuizSubmissionDto.Response getLatestSubmission(Long lectureItemId, String userEmail) {
        return quizSubmissionRepository
                .findTopByLectureItemIdAndUserEmailOrderByCreatedAtDesc(lectureItemId, userEmail)
                .map(this::toResponse)
                .orElse(null);
    }

    private QuizSubmissionDto.Response toResponse(QuizSubmission s) {
        return toResponse(s, null);
    }

    private QuizSubmissionDto.Response toResponse(QuizSubmission s, User user) {
        var response = QuizSubmissionDto.Response.builder()
                .id(s.getId())
                .lectureItemId(s.getLectureItem().getId())
                .answers(s.getAnswers() != null ? s.getAnswers().toString() : null)
                .totalPoints(s.getTotalPoints())
                .earnedPoints(s.getEarnedPoints())
                .createdAt(s.getCreatedAt())
                .build();
        if (user != null) {
            response.setCurrentHearts(user.getHearts());
            response.setHeartsRefillAt(user.getHeartsRefillAt());
        }
        return response;
    }
}
