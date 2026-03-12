package com.codehaja.domain.progress.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lecture_item_entry_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entry_progress_user_entry", columnNames = {"anonymous_user_id", "lecture_item_entry_id"})
        }
)
@Getter
@Setter
public class LectureItemEntryProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "anonymous_user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AnonymousUser anonymousUser;

    @JoinColumn(name = "lecture_item_entry_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LectureItemEntry lectureItemEntry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProgressStatus status;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    private Integer score;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "answer_json", columnDefinition = "TEXT")
    private JsonNode answerJson;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}