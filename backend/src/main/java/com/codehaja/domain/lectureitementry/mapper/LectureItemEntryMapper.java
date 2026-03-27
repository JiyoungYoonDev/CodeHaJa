package com.codehaja.domain.lectureitementry.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codehaja.domain.lectureitementry.dto.LectureItemEntryDto;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LectureItemEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lectureItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    LectureItemEntry toEntity(LectureItemEntryDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lectureItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    void updateEntityFromDto(LectureItemEntryDto.UpdateRequest request, @MappingTarget LectureItemEntry entry);

    default JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    default String map(JsonNode value) {
        return value != null ? value.toString() : null;
    }

    @Mapping(target = "lectureItemId", source = "lectureItem.id")
    @Mapping(target = "lectureItemTitle", source = "lectureItem.title")
    @Mapping(target = "lectureId", source = "lectureItem.lecture.id")
    @Mapping(target = "lectureTitle", source = "lectureItem.lecture.title")
    @Mapping(target = "courseSectionId", source = "lectureItem.lecture.courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "lectureItem.lecture.courseSection.title")
    @Mapping(target = "courseId", source = "lectureItem.lecture.courseSection.course.id")
    @Mapping(target = "courseTitle", source = "lectureItem.lecture.courseSection.course.title")
    @Mapping(target = "contentJson", expression = "java(map(entry.getContentJson()))")
    LectureItemEntryDto.DetailResponse toDetailResponse(LectureItemEntry entry);

    @Mapping(target = "lectureItemId", source = "lectureItem.id")
    @Mapping(target = "lectureItemTitle", source = "lectureItem.title")
    @Mapping(target = "lectureId", source = "lectureItem.lecture.id")
    @Mapping(target = "lectureTitle", source = "lectureItem.lecture.title")
    LectureItemEntryDto.SummaryResponse toSummaryResponse(LectureItemEntry entry);
}