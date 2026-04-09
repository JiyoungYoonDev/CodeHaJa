package com.codehaja.domain.interaction.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lecture_item_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reaction_user_item", columnNames = {"user_id", "lecture_item_id"})
}, indexes = {
        @Index(name = "idx_reaction_item", columnList = "lecture_item_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LectureItemReaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_item_id", nullable = false)
    private LectureItem lectureItem;

    /** LIKE or DISLIKE */
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 10)
    private ReactionType reactionType;
}
