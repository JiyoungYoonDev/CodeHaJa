package com.codehaja.domain.project.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.project.dto.ProjectSubmissionDto;
import com.codehaja.domain.project.entity.ProjectSubmission;
import com.codehaja.domain.project.repository.ProjectSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectSubmissionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final ProjectSubmissionRepository projectSubmissionRepository;

    @Transactional
    public ProjectSubmissionDto.Response submit(Long lectureItemId, ProjectSubmissionDto.CreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        ProjectSubmission submission = new ProjectSubmission();
        submission.setUser(user);
        submission.setLectureItem(lectureItem);

        if (request.getSubmissionData() != null && !request.getSubmissionData().isBlank()) {
            try {
                submission.setSubmissionData(OBJECT_MAPPER.readTree(request.getSubmissionData()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid submissionData JSON.");
            }
        }

        return toResponse(projectSubmissionRepository.save(submission));
    }

    public ProjectSubmissionDto.Response getLatestSubmission(Long lectureItemId, String userEmail) {
        return projectSubmissionRepository
                .findTopByLectureItemIdAndUserEmailOrderByCreatedAtDesc(lectureItemId, userEmail)
                .map(this::toResponse)
                .orElse(null);
    }

    public boolean hasSubmitted(Long lectureItemId, String userEmail) {
        return projectSubmissionRepository.existsByLectureItemIdAndUserEmail(lectureItemId, userEmail);
    }

    private ProjectSubmissionDto.Response toResponse(ProjectSubmission s) {
        return ProjectSubmissionDto.Response.builder()
                .id(s.getId())
                .lectureItemId(s.getLectureItem().getId())
                .submissionData(s.getSubmissionData() != null ? s.getSubmissionData().toString() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
