package com.codehaja.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.codehaja.model.CourseCategory;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long>{
    
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    Optional<CourseCategory> findByCategoryNameIgnoreCase(String categoryName);

    @Query("""
            SELECT c.id, c.categoryName, COUNT(pd.id)
            FROM CourseCategory c
            LEFT JOIN c.problemsBooks pd
            GROUP BY c.id, c.categoryName
            ORDER BY c.categoryName ASC
            """)
    List<Object[]> findAllWithProblemBookCount();
}
