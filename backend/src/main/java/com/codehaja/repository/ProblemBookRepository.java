package com.codehaja.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codehaja.model.ProblemsBook;

@Repository
public interface ProblemBookRepository extends JpaRepository<ProblemsBook, Long> {
    List<ProblemsBook> findByCourseCategory_CategoryNameIgnoreCase(String category);
}
