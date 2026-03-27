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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final AdminRepository adminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Transactional
    public CmsAuthDto.MeResponse login(CmsAuthDto.LoginRequest request, HttpServletResponse response) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        // Check lockout
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = Duration.between(LocalDateTime.now(), admin.getLockedUntil()).toMinutes() + 1;
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                    "Account locked. Try again in " + minutesLeft + " minute(s).");
        }

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            int attempts = admin.getFailedAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                admin.setFailedAttempts(0);
                admin.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                adminRepository.save(admin);
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                        "Too many failed attempts. Account locked for " + LOCK_DURATION_MINUTES + " minutes.");
            }
            admin.setFailedAttempts(attempts);
            adminRepository.save(admin);
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Invalid email or password. " + remaining + " attempt(s) remaining.");
        }

        // Success — reset lockout state
        admin.setFailedAttempts(0);
        admin.setLockedUntil(null);
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

    public CmsAuthDto.MeResponse getMe(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return toMeResponse(admin);
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
