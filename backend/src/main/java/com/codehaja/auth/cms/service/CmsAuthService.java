package com.codehaja.auth.cms.service;

import com.codehaja.auth.cms.dto.CmsAuthDto;
import com.codehaja.auth.cms.entity.Admin;
import com.codehaja.auth.cms.repository.AdminRepository;
import com.codehaja.auth.service.JwtService;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsAuthService {

    private final AdminRepository adminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Transactional
    public CmsAuthDto.MeResponse login(CmsAuthDto.LoginRequest request, String ip, HttpServletResponse response) {
        // IP-based lockout check (covers non-existent emails too)
        if (loginAttemptService.isBlocked(ip)) {
            long minutesLeft = loginAttemptService.getLockedMinutesLeft(ip);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                    "Too many failed attempts. Try again in " + minutesLeft + " minute(s).");
        }

        // Check email exists in admin table
        Admin admin = adminRepository.findByEmail(request.getEmail()).orElse(null);

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            loginAttemptService.recordFailure(ip);
            int remaining = loginAttemptService.getRemainingAttempts(ip);
            if (remaining == 0) {
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                        "Too many failed attempts. Account locked for 15 minutes.");
            }
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Invalid email or password. " + remaining + " attempt(s) remaining.");
        }

        // Success
        loginAttemptService.recordSuccess(ip);
        issueTokens(admin, response);
        return toMeResponse(admin);
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        clearCookie("access_token", "/", response);
        clearCookie("refresh_token", "/api/cms/auth/refresh", response);
        if (refreshToken != null) {
            adminRepository.findAll().stream()
                    .filter(a -> refreshToken.equals(a.getRefreshTokenHash()))
                    .findFirst()
                    .ifPresent(a -> {
                        a.setRefreshTokenHash(null);
                        a.setRefreshTokenExpiresAt(null);
                        adminRepository.save(a);
                    });
        }
    }

    @Transactional
    public CmsAuthDto.MeResponse createAdmin(CmsAuthDto.CreateAdminRequest request) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        Admin admin = Admin.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .failedAttempts(0)
                .build();
        adminRepository.save(admin);

        return toMeResponse(admin);
    }

    public CmsAuthDto.MeResponse getMe(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return toMeResponse(admin);
    }

    /**
     * admin이 1명이라도 있으면 true
     */
    public boolean hasAnyAdmin() {
        return adminRepository.count() > 0;
    }

    private void issueTokens(Admin admin, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(admin.getEmail());
        String refreshToken = jwtService.generateRefreshToken(admin.getEmail());

        admin.setRefreshTokenHash(refreshToken);
        admin.setRefreshTokenExpiresAt(
                LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        adminRepository.save(admin);

        addCookie("access_token", accessToken, (int) (accessTokenExpirationMs / 1000), "/", response);
        addCookie("refresh_token", refreshToken, (int) (refreshTokenExpirationMs / 1000), "/api/cms/auth/refresh", response);
    }

    private void addCookie(String name, String value, int maxAge, String path, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofSeconds(maxAge))
                .path(path)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(String name, String path, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ZERO)
                .path(path)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private CmsAuthDto.MeResponse toMeResponse(Admin admin) {
        return CmsAuthDto.MeResponse.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .build();
    }
}
