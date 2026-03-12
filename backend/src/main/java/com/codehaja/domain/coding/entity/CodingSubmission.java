package com.codehaja.domain.coding.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "coding_submissions")
@Getter
@Setter
public class CodingSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "anonymous_user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AnonymousUser anonymousUser;

    @JoinColumn(name = "lecture_item_entry_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LectureItemEntry lectureItemEntry;

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Column(length = 30)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 30)
    private SubmissionStatus submissionStatus;

    @Column(name = "passed_count")
    private Integer passedCount;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "result_json", columnDefinition = "TEXT")
    private JsonNode resultJson;
}