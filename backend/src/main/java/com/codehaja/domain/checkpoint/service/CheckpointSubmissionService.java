package com.codehaja.domain.checkpoint.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.checkpoint.dto.CheckpointSubmissionDto;
import com.codehaja.domain.checkpoint.entity.CheckpointSubmission;
import com.codehaja.domain.checkpoint.repository.CheckpointSubmissionRepository;
import com.codehaja.domain.gamification.service.HeartService;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckpointSubmissionService {

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CheckpointSubmissionRepository checkpointSubmissionRepository;
    private final HeartService heartService;

    @Transactional
    public CheckpointSubmissionDto.Response submit(Long lectureItemId,
                                                    CheckpointSubmissionDto.SubmitRequest request,
                                                    String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        // Require heart before checkpoint attempt
        heartService.requireHeart(user);

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        CheckpointSubmission submission = new CheckpointSubmission();
        submission.setUser(user);
        submission.setLectureItem(lectureItem);
        submission.setBlockId(request.getBlockId());
        submission.setUserAnswer(request.getUserAnswer());
        submission.setCorrectAnswer(request.getCorrectAnswer());
        submission.setCorrect(request.isCorrect());

        // Deduct heart on wrong answer
        if (!request.isCorrect()) {
            heartService.deductHeart(user);
        }

        CheckpointSubmissionDto.Response response = toResponse(checkpointSubmissionRepository.save(submission));
        response.setCurrentHearts(user.getHearts());
        response.setHeartsRefillAt(user.getHeartsRefillAt());
        return response;
    }

    public CheckpointSubmissionDto.ItemSubmissions getSubmissions(Long lectureItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        List<CheckpointSubmissionDto.Response> responses = checkpointSubmissionRepository
                .findByUserIdAndLectureItemIdOrderByCreatedAtDesc(user.getId(), lectureItemId)
                .stream()
                .map(this::toResponse)
                .toList();

        return CheckpointSubmissionDto.ItemSubmissions.builder()
                .lectureItemId(lectureItemId)
                .submissions(responses)
                .build();
    }

    private CheckpointSubmissionDto.Response toResponse(CheckpointSubmission s) {
        return CheckpointSubmissionDto.Response.builder()
                .id(s.getId())
                .lectureItemId(s.getLectureItem().getId())
                .blockId(s.getBlockId())
                .userAnswer(s.getUserAnswer())
                .correctAnswer(s.getCorrectAnswer())
                .correct(s.isCorrect())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
