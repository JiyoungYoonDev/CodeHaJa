package com.codehaja.domain.lectureitem.mapper;

import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LectureItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lecture", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    LectureItem toEntity(LectureItemDto.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lecture", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "contentJson", expression = "java(parseJson(request.getContentJson()))")
    void updateEntityFromDto(LectureItemDto.UpdateRequest request, @MappingTarget LectureItem lectureItem);

    @Mapping(target = "lectureId", source = "lecture.id")
    @Mapping(target = "lectureTitle", source = "lecture.title")
    @Mapping(target = "courseSectionId", source = "lecture.courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "lecture.courseSection.title")
    @Mapping(target = "courseId", source = "lecture.courseSection.course.id")
    @Mapping(target = "courseTitle", source = "lecture.courseSection.course.title")
    @Mapping(target = "entryCount", expression = "java(0L)")
    @Mapping(target = "contentJson", expression = "java(lectureItem.getContentJson() != null ? lectureItem.getContentJson().toString() : null)")
    LectureItemDto.DetailResponse toDetailResponse(LectureItem lectureItem);

    @Mapping(target = "lectureId", source = "lecture.id")
    @Mapping(target = "lectureTitle", source = "lecture.title")
    @Mapping(target = "sectionId", source = "lecture.courseSection.id")
    @Mapping(target = "courseId", source = "lecture.courseSection.course.id")
    @Mapping(target = "entryCount", expression = "java(0L)")
    LectureItemDto.SummaryResponse toSummaryResponse(LectureItem lectureItem);

    default JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON content: " + e.getMessage());
        }
    }
}