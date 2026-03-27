package com.codehaja.auth.cms.controller;

import com.codehaja.auth.cms.dto.CmsAuthDto;
import com.codehaja.auth.cms.service.CmsAuthService;
import com.codehaja.common.api.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/auth")
@RequiredArgsConstructor
public class CmsAuthController {

    private final CmsAuthService cmsAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CmsAuthDto.MeResponse>> login(
            @Valid @RequestBody CmsAuthDto.LoginRequest request,
            HttpServletResponse response) {
        CmsAuthDto.MeResponse admin = cmsAuthService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refresh_token");
        cmsAuthService.logout(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success((Void) null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        // Only admins (in the admins table) can access CMS
        try {
            CmsAuthDto.MeResponse admin = cmsAuthService.getMe(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(admin));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(ApiResponse.error("CMS access denied."));
        }
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
