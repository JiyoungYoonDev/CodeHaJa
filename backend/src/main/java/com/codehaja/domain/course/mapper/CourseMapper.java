package com.codehaja.domain.course.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.codehaja.domain.course.dto.CourseDto;
import com.codehaja.domain.course.entity.Course;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Course toEntity(CourseDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CourseDto.UpdateRequest request, @MappingTarget Course course);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.categoryName")
    CourseDto.Response toResponse(Course course);
    
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "totalSections", ignore = true)
    @Mapping(target = "detailedCurriculum", ignore = true)
    CourseDto.DetailResponse toDetailResponse(Course course);
}
