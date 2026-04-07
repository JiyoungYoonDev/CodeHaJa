package com.codehaja.domain.progress.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lecture.entity.Lecture;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lecture_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lecture_progress_user_lecture", columnNames = {"user_id", "lecture_id"})
        }
)
@Getter
@Setter
public class LectureProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "lecture_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lecture lecture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProgressStatus status;

    @Column(name = "current_item_order")
    private Integer currentItemOrder;

    @Column(name = "current_entry_order")
    private Integer currentEntryOrder;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}