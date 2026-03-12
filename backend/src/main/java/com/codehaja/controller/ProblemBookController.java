package com.codehaja.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codehaja.model.ProblemsBook;
import com.codehaja.dto.ProblemBookDto;
import com.codehaja.service.ProblemBookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problem-books")
@RequiredArgsConstructor
public class ProblemBookController {
    private final ProblemBookService problemBookService;

    @GetMapping
    public ResponseEntity<List<ProblemsBook>> getAllProblemBooks() {
        List<ProblemsBook> problemBooks = problemBookService.getAllProblemBooks();
        return ResponseEntity.ok(problemBooks);
    }

    // @GetMapping("/category/{category}")
    // public ResponseEntity<List<ProblemsBook>> getProblemBooksByCategory(@PathVariable String category) {
    //     List<ProblemsBook> problemBooks = problemBookService.getProblemBooksByCategory(category);
    //     return ResponseEntity.ok(problemBooks);
    // }

    @GetMapping("/{bookId}")
    public ResponseEntity<ProblemsBook> getProblemBookById(@PathVariable Long bookId) {
        List<ProblemsBook> problemBooks = problemBookService.getProblemBooksById(bookId);
        if (!problemBooks.isEmpty()) {
            return ResponseEntity.ok(problemBooks.get(0));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping
    public ResponseEntity<?> createProblemBook(@RequestBody ProblemBookDto.CreateRequest request) {
        try {
            ProblemBookDto.Response response = problemBookService.createProblemBook(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
