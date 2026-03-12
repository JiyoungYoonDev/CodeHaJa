package com.codehaja.domain.lectureitementry.mapper;

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
    LectureItemEntry toEntity(LectureItemEntryDto.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lectureItem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(LectureItemEntryDto.UpdateRequest request, @MappingTarget LectureItemEntry entry);

    @Mapping(target = "lectureItemId", source = "lectureItem.id")
    @Mapping(target = "lectureItemTitle", source = "lectureItem.title")
    @Mapping(target = "lectureId", source = "lectureItem.lecture.id")
    @Mapping(target = "lectureTitle", source = "lectureItem.lecture.title")
    @Mapping(target = "courseSectionId", source = "lectureItem.lecture.courseSection.id")
    @Mapping(target = "courseSectionTitle", source = "lectureItem.lecture.courseSection.title")
    @Mapping(target = "courseId", source = "lectureItem.lecture.courseSection.course.id")
    @Mapping(target = "courseTitle", source = "lectureItem.lecture.courseSection.course.title")
    LectureItemEntryDto.DetailResponse toDetailResponse(LectureItemEntry entry);

    @Mapping(target = "lectureItemId", source = "lectureItem.id")
    @Mapping(target = "lectureItemTitle", source = "lectureItem.title")
    @Mapping(target = "lectureId", source = "lectureItem.lecture.id")
    @Mapping(target = "lectureTitle", source = "lectureItem.lecture.title")
    LectureItemEntryDto.SummaryResponse toSummaryResponse(LectureItemEntry entry);
}