package com.codehaja.domain.anonymous.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.anonymous.dto.AnonymousUserDto;
import com.codehaja.domain.anonymous.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anonymous-users")
@RequiredArgsConstructor
public class AnonymousUserController {

    private final AnonymousUserService anonymousUserService;

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<AnonymousUserDto.Response>> initAnonymousUser(
            @RequestBody(required = false) AnonymousUserDto.InitRequest request
    ) {
        AnonymousUserDto.Response response = anonymousUserService.initAnonymousUser(request);
        return ResponseEntity.ok(ApiResponse.ok("Anonymous user initialized successfully.", response));
    }
}