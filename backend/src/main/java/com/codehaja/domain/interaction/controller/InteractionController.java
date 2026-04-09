package com.codehaja.domain.interaction.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.interaction.dto.InteractionDto;
import com.codehaja.domain.interaction.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // ── Hints ──

    @PostMapping("/items/{lectureItemId}/hints")
    public ResponseEntity<ApiResponse<InteractionDto.HintStatusResponse>> revealHint(
            @PathVariable Long lectureItemId,
            @RequestBody InteractionDto.HintRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InteractionDto.HintStatusResponse response =
                interactionService.revealHint(lectureItemId, request.getHintIndex(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Hint revealed.", response));
    }

    @GetMapping("/items/{lectureItemId}/hints")
    public ResponseEntity<ApiResponse<InteractionDto.HintStatusResponse>> getHintStatus(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        InteractionDto.HintStatusResponse response =
                interactionService.getHintStatus(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ── Reactions ──

    @PostMapping("/items/{lectureItemId}/reactions")
    public ResponseEntity<ApiResponse<InteractionDto.ReactionStatusResponse>> react(
            @PathVariable Long lectureItemId,
            @RequestBody InteractionDto.ReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InteractionDto.ReactionStatusResponse response =
                interactionService.react(lectureItemId, request.getReactionType(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Reaction saved.", response));
    }

    @GetMapping("/items/{lectureItemId}/reactions")
    public ResponseEntity<ApiResponse<InteractionDto.ReactionStatusResponse>> getReactionStatus(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        InteractionDto.ReactionStatusResponse response =
                interactionService.getReactionStatus(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
