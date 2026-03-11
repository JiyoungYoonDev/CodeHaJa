package com.codehaja.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import java.util.List;

import com.codehaja.dto.ProblemDto;
import com.codehaja.model.Problems;
import com.codehaja.repository.ProblemBookRepository;
import com.codehaja.repository.ProblemRepository;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final ProblemBookRepository problemBookRepository;

    public List<Problems> getAllProblems() {
        return problemRepository.findAll();
    }

    public Problems getProblemsById(Long id) {
        return problemRepository.findById(id).orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
    }

    public List<Problems> getProblemsByDifficulty(int difficulty) {
        return problemRepository.findByDifficulty(difficulty);
    }

    @Transactional
    public Problems createProblem(ProblemDto.CreateRequest problem) {
        Problems newProblem = new Problems();
        newProblem.setBookId(problemBookRepository.findById(problem.getBookId())
                .orElseThrow(() -> new RuntimeException("Problem book not found with id: " + problem.getBookId())));
        newProblem.setProblemTitle(problem.getTitle());
        newProblem.setProblemContent(problem.getContent());
        newProblem.setProblemAnswer(problem.getAnswer());
        newProblem.setProblemHints(problem.getHint());
        newProblem.setProblemCodeSkeleton(problem.getSkeletonCode());
        newProblem.setDifficulty(problem.getDifficulty());
        return problemRepository.save(newProblem);
    }
}
