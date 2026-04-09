package com.codehaja.domain.interaction.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hint_usages", indexes = {
        @Index(name = "idx_hint_user_item", columnList = "user_id, lecture_item_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_hint_user_item_index", columnNames = {"user_id", "lecture_item_id", "hint_index"})
})
@Getter
@Setter
@NoArgsConstructor
public class HintUsage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_item_id", nullable = false)
    private LectureItem lectureItem;

    /** 0-based index of which hint was revealed */
    @Column(name = "hint_index", nullable = false)
    private Integer hintIndex;
}
