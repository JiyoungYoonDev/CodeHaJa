package com.codehaja.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.codehaja.service.ProblemService;

import lombok.RequiredArgsConstructor;

import com.codehaja.dto.ProblemDto;
import com.codehaja.model.Problems;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemService problemService;
    
    @GetMapping
    public ResponseEntity<List<Problems>> getAllProblems() {
        List<Problems> problems = problemService.getAllProblems();
        return ResponseEntity.ok(problems);
    }

    @PostMapping
    public ResponseEntity<Problems> createProblem(@RequestBody ProblemDto.CreateRequest dto) {
        System.out.println("Received ProblemRequestDto: " + dto);
        Problems savedProblem = problemService.createProblem(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProblem);
    }

}
