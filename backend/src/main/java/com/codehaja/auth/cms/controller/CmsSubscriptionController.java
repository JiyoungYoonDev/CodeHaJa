package com.codehaja.auth.cms.controller;

import com.codehaja.auth.cms.service.CmsSubscriptionService;
import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.subscription.dto.SubscriptionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/subscriptions")
@RequiredArgsConstructor
public class CmsSubscriptionController {

    private final CmsSubscriptionService cmsSubscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SubscriptionDto.SubscriptionListItem>>> getSubscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsSubscriptionService.getSubscriptions(status, page, size)
        ));
    }

    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<SubscriptionDto.SubscriptionListItem>> grantSubscription(
            @RequestBody SubscriptionDto.GrantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsSubscriptionService.grantSubscription(request)
        ));
    }

    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionDto.SubscriptionListItem>> cancelSubscription(
            @PathVariable Long subscriptionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsSubscriptionService.cancelSubscription(subscriptionId)
        ));
    }
}
