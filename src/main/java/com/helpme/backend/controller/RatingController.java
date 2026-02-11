package com.helpme.backend.controller;

import com.helpme.backend.dto.ProviderRatingStatsDTO;
import com.helpme.backend.dto.RateProviderRequest;
import com.helpme.backend.dto.RatingDTO;
import com.helpme.backend.entity.User;
import com.helpme.backend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * POST /v1/ratings/orders/{orderId}
     * Driver đánh giá provider
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<RatingDTO> rateProvider(
            @PathVariable UUID orderId,
            @RequestBody @Valid RateProviderRequest request,
            @AuthenticationPrincipal User currentUser) {
        RatingDTO rating = ratingService.rateProvider(orderId, request, currentUser);
        return ResponseEntity.ok(rating);
    }

    /**
     * GET /v1/ratings/providers/{providerId}
     * Lấy danh sách ratings của provider
     */
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<RatingDTO>> getProviderRatings(
            @PathVariable UUID providerId) {
        List<RatingDTO> ratings = ratingService.getProviderRatings(providerId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * GET /v1/ratings/providers/{providerId}/stats
     * Lấy thống kê ratings của provider
     */
    @GetMapping("/providers/{providerId}/stats")
    public ResponseEntity<ProviderRatingStatsDTO> getProviderRatingStats(
            @PathVariable UUID providerId) {
        ProviderRatingStatsDTO stats = ratingService.getProviderRatingStats(providerId);
        return ResponseEntity.ok(stats);
    }
}