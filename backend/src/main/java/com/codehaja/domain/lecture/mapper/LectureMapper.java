package com.codehaja.domain.lecture.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    Lecture toEntity(LectureDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseSection", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    void updateEntityFromDto(LectureDto.UpdateRequest request, @MappingTarget Lecture lecture);

    default JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

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