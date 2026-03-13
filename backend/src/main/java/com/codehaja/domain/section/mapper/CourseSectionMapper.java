package com.codehaja.domain.section.mapper;

import com.codehaja.domain.course.dto.CourseDto;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.section.dto.CourseSectionDto;
import com.codehaja.domain.section.entity.CourseSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CourseSectionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CourseSection toEntity(CourseSectionDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CourseSection toEntity(CourseSectionDto.UpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CourseSectionDto.UpdateRequest request, @MappingTarget CourseSection section);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "lectureCount", expression = "java(0L)")
    CourseSectionDto.DetailResponse toDetailResponse(CourseSection section);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "lectureCount", expression = "java(0L)")
    CourseSectionDto.SummaryResponse toSummaryResponse(CourseSection section);
}