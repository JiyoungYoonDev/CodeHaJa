package com.codehaja.auth.controller;

import com.codehaja.auth.dto.AuthDto;
import com.codehaja.auth.service.AuthService;
import com.codehaja.common.api.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthDto.MeResponse>> signup(
            @RequestBody AuthDto.SignupRequest request,
            HttpServletResponse response) {
        AuthDto.MeResponse user = authService.signup(request, response);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.MeResponse>> login(
            @RequestBody AuthDto.LoginRequest request,
            HttpServletResponse response) {
        AuthDto.MeResponse user = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthDto.MeResponse>> googleLogin(
            @RequestBody AuthDto.GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthDto.MeResponse user = authService.googleLogin(request, response);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.success((Void) null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.MeResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refresh_token");
        AuthDto.MeResponse user = authService.refresh(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        AuthDto.MeResponse user = authService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody AuthDto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success((Void) null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody AuthDto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success((Void) null));
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
