package com.codehaja.auth.cms.controller;

import com.codehaja.auth.cms.dto.CmsUserDto;
import com.codehaja.auth.cms.service.CmsUserService;
import com.codehaja.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/users")
@RequiredArgsConstructor
public class CmsUserController {

    private final CmsUserService cmsUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CmsUserDto.UserListItem>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsUserService.getUsers(search, role, status, page, size)
        ));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<CmsUserDto.UserListItem>> updateRole(
            @PathVariable Long userId,
            @RequestBody CmsUserDto.RoleUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsUserService.updateRole(userId, request.getRole())
        ));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<CmsUserDto.UserListItem>> updateStatus(
            @PathVariable Long userId,
            @RequestBody CmsUserDto.StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cmsUserService.updateStatus(userId, request.getStatus())
        ));
    }
}
