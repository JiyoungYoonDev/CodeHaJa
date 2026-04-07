package com.codehaja.auth.cms.controller;

import com.codehaja.auth.cms.dto.CmsAuthDto;
import com.codehaja.auth.cms.service.CmsAuthService;
import com.codehaja.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/admins")
@RequiredArgsConstructor
public class CmsAdminController {

    private final CmsAuthService cmsAuthService;

    @PostMapping
    public ResponseEntity<ApiResponse<CmsAuthDto.MeResponse>> createAdmin(
            @Valid @RequestBody CmsAuthDto.CreateAdminRequest request) {
        CmsAuthDto.MeResponse created = cmsAuthService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }
}
