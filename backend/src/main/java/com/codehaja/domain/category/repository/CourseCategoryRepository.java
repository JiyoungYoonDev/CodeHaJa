package com.codehaja.domain.category.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.codehaja.domain.category.entity.CourseCategory;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long>{
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    Optional<CourseCategory> findByCategoryNameIgnoreCase(String categoryName);

    @Query("""
        SELECT c.id, c.categoryName, COUNT(course.id)
        FROM CourseCategory c
        LEFT JOIN Course course ON course.category.id = c.id
        GROUP BY c.id, c.categoryName
        ORDER BY c.categoryName ASC
    """)
    List<Object[]> findAllWithCourseCount();

    @Query("SELECT COUNT(course.id) FROM Course course WHERE course.category.id = :categoryId")
    long countCoursesByCategoryId(Long categoryId);

}
