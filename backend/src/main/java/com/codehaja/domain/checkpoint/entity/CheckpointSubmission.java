package com.codehaja.domain.checkpoint.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checkpoint_submissions",
       indexes = @Index(name = "idx_cp_sub_user_item_block",
                        columnList = "user_id, lecture_item_id, block_id"))
@Getter
@Setter
public class CheckpointSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LectureItem lectureItem;

    /** UUID of the checkpoint block within the item's blocks array */
    @Column(name = "block_id", nullable = false, length = 64)
    private String blockId;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "correct_answer", length = 500)
    private String correctAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;
}
