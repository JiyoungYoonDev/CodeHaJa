package com.codehaja.domain.gamification.controller;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.gamification.service.HeartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hearts")
@RequiredArgsConstructor
public class HeartController {

    private final UserRepository userRepository;
    private final HeartService heartService;

    @PostMapping("/deduct")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deduct(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        heartService.requireHeart(user);
        heartService.deductHeart(user);

        Map<String, Object> data = Map.of(
                "currentHearts", user.getHearts(),
                "heartsRefillAt", user.getHeartsRefillAt() != null
                        ? user.getHeartsRefillAt().toString() : ""
        );
        return ResponseEntity.ok(ApiResponse.ok("Heart deducted.", data));
    }
}
