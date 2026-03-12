package com.codehaja.domain.lecture.mapper;

import com.codehaja.domain.lecture.dto.LectureDto;
import com.codehaja.domain.lecture.entity.Lecture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LectureMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseSection", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lecture toEntity(LectureDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseSection", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(LectureDto.UpdateRequest request, @MappingTarget Lecture lecture);

    @Mapping(target = "courseSectionId", source = "courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "courseSection.title")
    @Mapping(target = "courseId", source = "courseSection.course.id")
    @Mapping(target = "courseTitle", source = "courseSection.course.title")
    @Mapping(target = "itemCount", expression = "java(0L)")
    LectureDto.DetailResponse toDetailResponse(Lecture lecture);

    @Mapping(target = "courseSectionId", source = "courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "courseSection.title")
    @Mapping(target = "itemCount", expression = "java(0L)")
    LectureDto.SummaryResponse toSummaryResponse(Lecture lecture);
}