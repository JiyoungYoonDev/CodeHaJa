package com.codehaja.domain.quiz.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_submissions")
@Getter
@Setter
public class QuizSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_item_id", nullable = false)
    private LectureItem lectureItem;

    // Array of { blockId, quizType, answer, isCorrect, points }
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "answers", columnDefinition = "TEXT")
    private JsonNode answers;

    @Column(name = "total_points")
    private int totalPoints;

    @Column(name = "earned_points")
    private int earnedPoints;
}
