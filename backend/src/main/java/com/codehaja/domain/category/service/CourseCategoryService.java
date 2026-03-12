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
        List<CourseCategory> categories = courseCategoryRepository.findAll();
        List<CourseCategoryDto.Response> responseList = new ArrayList<>();

        for (CourseCategory category : categories) {
            responseList.add(toResponse(category, 0L));
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

    private CourseCategoryDto.Response toResponse(CourseCategory category, Long problemBookCount) {
        CourseCategoryDto.Response response = new CourseCategoryDto.Response();
        response.setId(category.getId());
        response.setCategoryName(category.getCategoryName());
        response.setProblemBookCount(problemBookCount);

        return response;
    }
}
