package com.codehaja.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.codehaja.dto.CourseCategoryDto;
import com.codehaja.model.CourseCategory;
import com.codehaja.repository.CourseCategoryRepository;

@Service
public class CourseCategoryService {
    private final CourseCategoryRepository courseCategoryRepository;

    public CourseCategoryService(CourseCategoryRepository courseCategoryRepository) {
        this.courseCategoryRepository = courseCategoryRepository;
    }

    public CourseCategoryDto.Response createCategory(CourseCategoryDto.Request request) {
        String categoryName = request.getCategoryName() == null ? "" : request.getCategoryName().trim();

        System.out.println("Creating category: " + request);

        if (categoryName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        if (courseCategoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new IllegalArgumentException("Category already exists.");
        }

        CourseCategory courseCategory = new CourseCategory();
        courseCategory.setCategoryName(categoryName);

        CourseCategory newCategory = courseCategoryRepository.save(courseCategory);

        CourseCategoryDto.Response response = new CourseCategoryDto.Response();
        response.setCategoryName(newCategory.getCategoryName());
        response.setId(newCategory.getId());
        response.setProblemBookCount(0L);

        return response;
    }

    public List<CourseCategoryDto.Response> getAllCategories() {
        List<Object[]> rows = courseCategoryRepository.findAllWithProblemBookCount();
        List<CourseCategoryDto.Response> responseList = new ArrayList<>();

        for (Object[] row : rows) {
            CourseCategoryDto.Response response = new CourseCategoryDto.Response();

            response.setId((Long) row[0]);
            response.setCategoryName((String) row[1]);
            response.setProblemBookCount((Long) row[2]);

            responseList.add(response);
        }

        return responseList;
    }
}