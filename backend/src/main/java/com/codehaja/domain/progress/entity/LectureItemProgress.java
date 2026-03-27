package com.codehaja.domain.progress.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lecture_item_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_progress_user_item", columnNames = {"user_id", "lecture_item_id"})
        }
)
@Getter
@Setter
public class LectureItemProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "lecture_item_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LectureItem lectureItem;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
