package com.codehaja.domain.category.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.category.dto.CourseCategoryDto;
import com.codehaja.domain.category.entity.CourseCategory;
import com.codehaja.domain.category.repository.CourseCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCategoryService {
    private final CourseCategoryRepository courseCategoryRepository;

    @Transactional
    public CourseCategoryDto.Response createCategory(CourseCategoryDto.Request request) {
        String categoryName = request.getCategoryName() == null ? "" : request.getCategoryName().trim();

        if (categoryName.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Category name cannot be empty.");
        }

        if (courseCategoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        CourseCategory category = new CourseCategory();
        category.setCategoryName(categoryName);

        CourseCategory savedCourseCategory = courseCategoryRepository.save(category);

        return toResponse(savedCourseCategory, 0L);
    }

    public List<CourseCategoryDto.Response> getAllCategories() {
        List<Object[]> rows = courseCategoryRepository.findAllWithCourseCount();
        List<CourseCategoryDto.Response> responseList = new ArrayList<>();

        for (Object[] row : rows) {
            CourseCategoryDto.Response response = new CourseCategoryDto.Response();
            response.setId(((Number) row[0]).longValue());
            response.setCategoryName((String) row[1]);
            response.setCourseCount(((Number) row[2]).longValue());
            responseList.add(response);
        }

        return responseList;
    }

    public CourseCategoryDto.Response getCategory(Long categoryId) {
        CourseCategory category = courseCategoryRepository.findById(categoryId).orElseThrow(
            () -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        
        return toResponse(category, 0L);
    }

    @Transactional
    public CourseCategoryDto.Response updateCategory(Long categoryId, CourseCategoryDto.Request request) {
        CourseCategory category = courseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        String categoryName = request.getCategoryName() == null ? "" : request.getCategoryName();

        if (categoryName.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Category name cannot be empty");
        }

        boolean alreadyExists = courseCategoryRepository.existsByCategoryNameIgnoreCase(categoryName);
        boolean sameName = category.getCategoryName().equalsIgnoreCase(categoryName);

        if(alreadyExists && !sameName) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        category.setCategoryName(categoryName);

        return toResponse(category, 0L);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        CourseCategory category = courseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        
        courseCategoryRepository.delete(category);
    }

    private CourseCategoryDto.Response toResponse(CourseCategory category, Long courseCount) {
        CourseCategoryDto.Response response = new CourseCategoryDto.Response();
        response.setId(category.getId());
        response.setCategoryName(category.getCategoryName());
        response.setCourseCount(courseCount);

        return response;
    }
}
