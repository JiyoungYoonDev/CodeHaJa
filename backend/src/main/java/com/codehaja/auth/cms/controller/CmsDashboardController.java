package com.codehaja.auth.cms.controller;

import com.codehaja.auth.cms.dto.CmsDashboardDto;
import com.codehaja.auth.cms.service.CmsDashboardService;
import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/dashboard")
@RequiredArgsConstructor
public class CmsDashboardController {

    private final CmsDashboardService cmsDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CmsDashboardDto.StatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(cmsDashboardService.getStats()));
    }

    @GetMapping("/content/items")
    public ResponseEntity<ApiResponse<Page<LectureItemDto.SummaryResponse>>> getContentItems(
            @RequestParam(defaultValue = "DRAFT") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(
                cmsDashboardService.getItemsByReviewStatus(reviewStatus, page, size)
        ));
    }
}
