package com.codehaja.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.codehaja.dto.ProblemBookDto;
import com.codehaja.model.CourseCategory;
import com.codehaja.model.CourseSection;
import com.codehaja.model.ProblemsBook;
import com.codehaja.repository.CourseCategoryRepository;
import com.codehaja.repository.ProblemBookRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProblemBookService {
    private final ProblemBookRepository problemBookRepository;
    private final CourseCategoryRepository courseCategoryRepository;

    @Transactional
    public ProblemBookDto.Response createProblemBook(ProblemBookDto.CreateRequest request) {
        ProblemsBook problemBook = new ProblemsBook();
        problemBook.setProblemTitle(request.getTitle());
        problemBook.setBookDescription(request.getBookDescription());
        problemBook.setCourseCategory(resolveCourseCategory(request.getCourseCategory()));
        problemBook.setBookDifficulty(defaultIfBlank(request.getDifficulty()));
        problemBook.setRating(request.getRating());
        problemBook.setBookCount(0L);
        problemBook.setBookProjectsCount(defaultIfNull(request.getProjectsCount()));
        problemBook.setHours(defaultIfNull(request.getHours()));
        problemBook.setLearnersCount(defaultIfNull(request.getLearnersCount()));
        problemBook.setBadgeType(request.getBadgeType());
        problemBook.setProvider(request.getProvider());
        problemBook.setImageUrl(request.getImageUrl());
        problemBook.setStatus(request.getStatus());
        problemBook.setDetailedCurriculum(request.getDetailedCurriculum());

        List<CourseSection> sectionEntities = new ArrayList<>();

        if (request.getCourseSections() != null) {
            for (ProblemBookDto.CourseSectionRequest sectionRequest : request.getCourseSections()) {
                CourseSection section = new CourseSection();
                section.setTitle(sectionRequest.getTitle());
                section.setDescription(sectionRequest.getDescription());
                section.setSubCount(defaultIfNull(sectionRequest.getSubCount()));
                section.setHours(defaultIfNull(sectionRequest.getHours()));
                section.setPoints(defaultIfNull(sectionRequest.getPoints()));
                section.setProblemBook(problemBook);

                sectionEntities.add(section);
            }
        }

        problemBook.setCourseSections(sectionEntities);

        ProblemsBook saved = problemBookRepository.save(problemBook);

        return toResponse(saved);
    }

    public List<ProblemsBook> getAllProblemBooks() {
        return problemBookRepository.findAll();
    }

    public List<ProblemsBook> getProblemBooksByCategory(String category) {
        return problemBookRepository.findByCourseCategory_CategoryNameIgnoreCase(category);
    }

    private ProblemBookDto.Response toResponse(ProblemsBook saved) {
        ProblemBookDto.Response response = new ProblemBookDto.Response();
        response.setId(saved.getId());
        response.setTitle(saved.getProblemTitle());
        response.setBookDescription(saved.getBookDescription());
        response.setCourseCategory(saved.getCourseCategory() == null ? null : saved.getCourseCategory().getCategoryName());
        response.setDifficulty(saved.getBookDifficulty());
        response.setRating(saved.getRating());
        response.setProjectsCount(saved.getBookProjectsCount());
        response.setHours(saved.getHours());
        response.setLearnersCount(saved.getLearnersCount());
        response.setBadgeType(saved.getBadgeType());
        response.setProvider(saved.getProvider());
        response.setImageUrl(saved.getImageUrl());
        response.setStatus(saved.getStatus());
        response.setDetailedCurriculum(saved.getDetailedCurriculum());

        List<ProblemBookDto.CourseSectionResponse> sections = new ArrayList<>();
        if (saved.getCourseSections() != null) {
            for (CourseSection section : saved.getCourseSections()) {
                ProblemBookDto.CourseSectionResponse sectionResponse = new ProblemBookDto.CourseSectionResponse();
                sectionResponse.setId(section.getId());
                sectionResponse.setTitle(section.getTitle());
                sectionResponse.setDescription(section.getDescription());
                sectionResponse.setSubCount(section.getSubCount());
                sectionResponse.setHours(section.getHours());
                sectionResponse.setPoints(section.getPoints());
                sections.add(sectionResponse);
            }
        }

        response.setCourseSections(sections);
        return response;
    }
    
    private Integer defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultIfBlank(String value) {
        return value == null || value.isBlank() ? "0" : value;
    }

    private CourseCategory resolveCourseCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Course category is required.");
        }

        return courseCategoryRepository
            .findByCategoryNameIgnoreCase(categoryName)
            .orElseGet(() -> {
                CourseCategory category = new CourseCategory();
                category.setCategoryName(categoryName.trim());
                return courseCategoryRepository.save(category);
            });
    }

    // public ProblemsBook getProblemBookByCategoryAndId(String category, Long id) {
    //     return problemBookRepository.findByProblemCategory(category).stream()
    //             .filter(book -> book.getId().equals(id))
    //             .findFirst()
    //             .orElseThrow(() -> new RuntimeException("문제집을 찾을 수 없습니다. 카테고리: " + category + ", ID: " + id));
    // }
}
