package com.codehaja.domain.coding.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.anonymous.service.AnonymousUserService;
import com.codehaja.domain.coding.dto.CodingSubmissionDto;
import com.codehaja.domain.coding.entity.CodingSubmission;
import com.codehaja.domain.coding.entity.SubmissionStatus;
import com.codehaja.domain.coding.repository.CodingSubmissionRepository;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import com.codehaja.domain.lectureitementry.repository.LectureItemEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodingSubmissionService {

    private final AnonymousUserService anonymousUserService;
    private final LectureItemEntryRepository lectureItemEntryRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;

    @Transactional
    public CodingSubmissionDto.Response submit(Long lectureItemEntryId, CodingSubmissionDto.SubmitRequest request) {
        validateRequest(request);

        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(request.getAnonymousUserKey());

        LectureItemEntry entry = lectureItemEntryRepository.findById(lectureItemEntryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTRY_NOT_FOUND));

        CodingSubmission submission = new CodingSubmission();
        submission.setAnonymousUser(anonymousUser);
        submission.setLectureItemEntry(entry);
        submission.setSourceCode(request.getSourceCode());
        submission.setLanguage(request.getLanguage());

        submission.setSubmissionStatus(SubmissionStatus.PENDING);
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setExecutionTimeMs(0L);
        submission.setStdout("");
        submission.setStderr("");
        submission.setResultJson(null);

        CodingSubmission saved = codingSubmissionRepository.save(submission);

        return toResponse(saved);
    }

    public CodingSubmissionDto.Response getSubmission(Long submissionId) {
        CodingSubmission submission = codingSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODING_SUBMISSION_NOT_FOUND));

        return toResponse(submission);
    }

    public List<CodingSubmissionDto.Response> getSubmissions(Long lectureItemEntryId, String anonymousUserKey) {
        AnonymousUser anonymousUser = anonymousUserService.getAnonymousUserOrThrow(anonymousUserKey);

        List<CodingSubmission> submissions = codingSubmissionRepository
                .findAllByAnonymousUserIdAndLectureItemEntryIdOrderByCreatedAtDesc(
                        anonymousUser.getId(), lectureItemEntryId
                );

        return submissions.stream().map(this::toResponse).toList();
    }

    private CodingSubmissionDto.Response toResponse(CodingSubmission submission) {
        CodingSubmissionDto.Response response = new CodingSubmissionDto.Response();
        response.setId(submission.getId());
        response.setLectureItemEntryId(submission.getLectureItemEntry().getId());
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

    private void validateRequest(CodingSubmissionDto.SubmitRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getAnonymousUserKey() == null || request.getAnonymousUserKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }
        if (request.getSourceCode() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sourceCode is required.");
        }
    }
}