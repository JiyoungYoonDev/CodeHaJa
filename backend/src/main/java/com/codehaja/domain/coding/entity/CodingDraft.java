package com.codehaja.domain.coding.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "coding_drafts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_coding_draft_user_entry", columnNames = {"anonymous_user_id", "lecture_item_entry_id"})
        }
)
@Getter
@Setter
public class CodingDraft extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "anonymous_user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AnonymousUser anonymousUser;

    @JoinColumn(name = "lecture_item_entry_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LectureItemEntry lectureItemEntry;

    @Column(name = "source_code", columnDefinition = "TEXT")
    private String sourceCode;

    @Column(length = 30)
    private String language;
}