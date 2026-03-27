package com.codehaja.domain.coding.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.coding.dto.CodingSubmissionDto;
import com.codehaja.domain.coding.entity.CodingSubmission;
import com.codehaja.domain.coding.entity.SubmissionStatus;
import com.codehaja.domain.coding.repository.CodingSubmissionRepository;
import com.codehaja.domain.judge.PistonClient;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodingSubmissionService {

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final PistonClient pistonClient;

    @Transactional
    public CodingSubmissionDto.Response submit(Long lectureItemId, CodingSubmissionDto.SubmitRequest request, String userEmail) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getSourceCode() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sourceCode is required.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        String expectedOutput = null;
        if (lectureItem.getContentJson() != null) {
            var node = lectureItem.getContentJson().get("expectedOutput");
            if (node != null && !node.isNull()) {
                expectedOutput = node.asText();
            }
        }

        CodingSubmission submission = new CodingSubmission();
        submission.setUser(user);
        submission.setLectureItem(lectureItem);
        submission.setSourceCode(request.getSourceCode());
        submission.setLanguage(request.getLanguage());
        submission.setSubmissionStatus(SubmissionStatus.RUNNING);
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setExecutionTimeMs(0L);
        submission.setStdout("");
        submission.setStderr("");
        submission.setResultJson(null);

        CodingSubmission saved = codingSubmissionRepository.save(submission);

        try {
            PistonClient.ExecutionResult result = pistonClient.execute(
                    request.getSourceCode(), request.getLanguage()
            );
            saved.setStdout(result.getStdout() != null ? result.getStdout() : "");
            saved.setStderr(result.getStderr() != null ? result.getStderr() : "");
            saved.setTotalCount(1);

            if (result.isCompileError()) {
                saved.setSubmissionStatus(SubmissionStatus.ERROR);
            } else if (result.isRuntimeError()) {
                saved.setSubmissionStatus(SubmissionStatus.ERROR);
            } else if (result.isAccepted(expectedOutput)) {
                saved.setSubmissionStatus(SubmissionStatus.PASSED);
                saved.setPassedCount(1);
            } else {
                saved.setSubmissionStatus(SubmissionStatus.FAILED);
            }
        } catch (Exception e) {
            saved.setSubmissionStatus(SubmissionStatus.ERROR);
            saved.setStderr("Execution failed: " + e.getMessage());
        }

        return toResponse(saved);
    }

    public CodingSubmissionDto.Response getSubmission(Long submissionId) {
        CodingSubmission submission = codingSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODING_SUBMISSION_NOT_FOUND));
        return toResponse(submission);
    }

    public CodingSubmissionDto.Response getLatestSubmission(Long lectureItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return codingSubmissionRepository
                .findAllByUserIdAndLectureItemIdOrderByCreatedAtDesc(user.getId(), lectureItemId)
                .stream().findFirst().map(this::toResponse).orElse(null);
    }

    public List<Long> getPassedItemIds(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return codingSubmissionRepository.findDistinctLectureItemIdsByUserIdAndStatusAndCourseId(
                user.getId(), SubmissionStatus.PASSED, courseId);
    }

    public List<CodingSubmissionDto.Response> getSubmissions(Long lectureItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        return codingSubmissionRepository
                .findAllByUserIdAndLectureItemIdOrderByCreatedAtDesc(user.getId(), lectureItemId)
                .stream().map(this::toResponse).toList();
    }

    private CodingSubmissionDto.Response toResponse(CodingSubmission submission) {
        CodingSubmissionDto.Response response = new CodingSubmissionDto.Response();
        response.setId(submission.getId());
        response.setLectureItemId(submission.getLectureItem().getId());
        response.setSourceCode(submission.getSourceCode());
        response.setLanguage(submission.getLanguage());
        response.setSubmissionStatus(submission.getSubmissionStatus().name());
        response.setPassedCount(submission.getPassedCount());
        response.setTotalCount(submission.getTotalCount());
        response.setExecutionTimeMs(submission.getExecutionTimeMs());
        response.setStdout(submission.getStdout());
        response.setStderr(submission.getStderr());
        response.setResultJson(submission.getResultJson());
        return response;
    }
}
