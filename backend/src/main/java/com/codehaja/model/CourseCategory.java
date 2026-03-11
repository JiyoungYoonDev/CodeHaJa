package com.codehaja.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CourseCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @OneToMany(mappedBy = "courseCategory")
    private List<ProblemsBook> problemsBooks;
}
