package com.codehaja.domain.project.entity;

import com.codehaja.auth.entity.User;
import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_submissions")
@Getter
@Setter
public class ProjectSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "lecture_item_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LectureItem lectureItem;

    // Stores REPO fields ({ github_url, demo_url, ... }) or EDITOR files ({ files: [...], language })
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "submission_data", columnDefinition = "TEXT")
    private JsonNode submissionData;
}
