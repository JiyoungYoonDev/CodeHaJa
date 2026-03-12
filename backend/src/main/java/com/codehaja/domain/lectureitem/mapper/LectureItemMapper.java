package com.codehaja.domain.lectureitem.mapper;

import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
    LectureItem toEntity(LectureItemDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lecture", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(LectureItemDto.UpdateRequest request, @MappingTarget LectureItem lectureItem);

    @Mapping(target = "lectureId", source = "lecture.id")
    @Mapping(target = "lectureTitle", source = "lecture.title")
    @Mapping(target = "courseSectionId", source = "lecture.courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "lecture.courseSection.title")
    @Mapping(target = "courseId", source = "lecture.courseSection.course.id")
    @Mapping(target = "courseTitle", source = "lecture.courseSection.course.title")
    @Mapping(target = "entryCount", expression = "java(0L)")
    LectureItemDto.DetailResponse toDetailResponse(LectureItem lectureItem);

    @Mapping(target = "lectureId", source = "lecture.id")
    @Mapping(target = "lectureTitle", source = "lecture.title")
    @Mapping(target = "entryCount", expression = "java(0L)")
    LectureItemDto.SummaryResponse toSummaryResponse(LectureItem lectureItem);
}