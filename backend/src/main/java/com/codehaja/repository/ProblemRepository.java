package com.codehaja.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codehaja.model.Problems;

public interface ProblemRepository extends JpaRepository<Problems, Long>{
    List<Problems> findByDifficulty(int difficulty);
}
