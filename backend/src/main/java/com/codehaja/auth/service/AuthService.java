package com.codehaja.auth.service;

import com.codehaja.auth.dto.AuthDto;
import com.codehaja.auth.entity.AuthProvider;
import com.codehaja.auth.entity.User;
import com.codehaja.auth.entity.UserRole;
import com.codehaja.auth.entity.UserStatus;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
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
    public AuthDto.MeResponse signup(AuthDto.SignupRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .provider(AuthProvider.LOCAL)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        issueTokens(saved, response);
        return toMeResponse(saved);
    }

    @Transactional
    public AuthDto.MeResponse login(AuthDto.LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.getProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_BANNED);
        }

        issueTokens(user, response);
        return toMeResponse(user);
    }

    @Transactional
    public AuthDto.MeResponse googleLogin(AuthDto.GoogleLoginRequest request, HttpServletResponse response) {
        Map<String, Object> googleUser = verifyGoogleToken(request.getIdToken());

        String email = (String) googleUser.get("email");
        String name = (String) googleUser.get("name");
        String picture = (String) googleUser.get("picture");
        String googleId = (String) googleUser.get("sub");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileImage(picture)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(googleId)
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            return userRepository.save(newUser);
        });

        issueTokens(user, response);
        return toMeResponse(user);
    }

    @Transactional
    public AuthDto.MeResponse refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

        if (user.getRefreshTokenHash() == null ||
                !hashToken(refreshToken).equals(user.getRefreshTokenHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        if (user.getRefreshTokenExpiresAt() == null ||
                user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        issueTokens(user, response);
        return toMeResponse(user);
    }

    public void logout(HttpServletResponse response) {
        clearCookie("access_token", response);
        clearCookie("refresh_token", response);
    }

    public AuthDto.MeResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return toMeResponse(user);
    }

    @Transactional
    public void forgotPassword(AuthDto.ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // TODO: Send reset email
            log.info("Password reset token for {}: {}", user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> request.getToken().equals(u.getPasswordResetToken()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

        if (user.getPasswordResetTokenExpiresAt() == null ||
                user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    private void issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        user.setRefreshTokenHash(hashToken(refreshToken));
        user.setRefreshTokenExpiresAt(
                LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);

        addCookie("access_token", accessToken, (int) (accessTokenExpirationMs / 1000), "/", response);
        addCookie("refresh_token", refreshToken, (int) (refreshTokenExpirationMs / 1000), "/api/auth/refresh", response);
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

    private void clearCookie(String name, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ZERO)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result == null || result.get("email") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private AuthDto.MeResponse toMeResponse(User user) {
        return AuthDto.MeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .role(user.getRole().name())
                .provider(user.getProvider().name())
                .build();
    }
}
