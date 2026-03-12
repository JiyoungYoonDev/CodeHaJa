package com.codehaja.domain.category.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.codehaja.domain.category.entity.CourseCategory;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long>{
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    Optional<CourseCategory> findByCategoryNameIgnoreCase(String categoryName);
}
