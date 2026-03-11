package com.codehaja.model;
import java.util.*;
import jakarta.persistence.*;

@Entity
public class RoadMap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "order_index")
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problems_book_id")
    private ProblemsBook problemsBook;

    @OneToMany(mappedBy = "roadMap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();
}
