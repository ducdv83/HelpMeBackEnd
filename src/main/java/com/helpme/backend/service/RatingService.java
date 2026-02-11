package com.helpme.backend.service;

import com.helpme.backend.dto.ProviderRatingStatsDTO;
import com.helpme.backend.dto.RateProviderRequest;
import com.helpme.backend.dto.RatingDTO;
import com.helpme.backend.entity.*;
import com.helpme.backend.exception.BadRequestException;
import com.helpme.backend.exception.ForbiddenException;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    /**
     * Driver đánh giá provider sau khi order COMPLETED
     */
    public RatingDTO rateProvider(UUID orderId, RateProviderRequest request, User driver) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Validate driver ownership
        if (!order.getDriverId().equals(driver.getId())) {
            throw new ForbiddenException("Not your order");
        }

        // Validate order completed
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Can only rate completed orders");
        }

        // Validate provider exists
        if (order.getProviderId() == null) {
            throw new BadRequestException("No provider assigned to this order");
        }

        // Check if already rated
        if (ratingRepository.existsByOrderId(orderId)) {
            throw new BadRequestException("Order already rated");
        }

        // Create rating
        Rating rating = new Rating();
        rating.setOrderId(order.getId());
        rating.setProviderId(order.getProviderId());
        rating.setRating(request.getRating());
        rating.setComment(request.getComment());

        Rating savedRating = ratingRepository.save(rating);

        // Update provider average rating
        updateProviderRating(order.getProviderId());

        log.info("✅ Order {} rated with {} stars by driver {}",
                orderId, request.getRating(), driver.getId());

        return toRatingDTO(savedRating);
    }

    /**
     * Lấy ratings của provider
     */
    public List<RatingDTO> getProviderRatings(UUID providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        List<Rating> ratings = ratingRepository.findByProviderIdOrderByCreatedAtDesc(providerId);

        return ratings.stream()
                .map(this::toRatingDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê ratings của provider
     */
    public ProviderRatingStatsDTO getProviderRatingStats(UUID providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        Double avgRating = ratingRepository.calculateAverageRating(providerId);
        List<Object[]> breakdown = ratingRepository.countRatingsByLevel(providerId);

        // Convert breakdown to Map
        Map<Integer, Long> ratingBreakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingBreakdown.put(i, 0L);
        }

        breakdown.forEach(row -> {
            Integer ratingLevel = (Integer) row[0];
            Long count = (Long) row[1];
            ratingBreakdown.put(ratingLevel, count);
        });

        Long totalRatings = ratingBreakdown.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return ProviderRatingStatsDTO.builder()
                .averageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .totalRatings(totalRatings)
                .ratingBreakdown(ratingBreakdown)
                .build();
    }

    /**
     * Update provider average rating
     */
    private void updateProviderRating(UUID providerId) {
        Double avgRating = ratingRepository.calculateAverageRating(providerId);

        if (avgRating != null) {
            Provider provider = providerRepository.findById(providerId)
                    .orElseThrow(() -> new NotFoundException("Provider not found"));

            provider.setRatingAvg(
                    BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));

            providerRepository.save(provider);

            log.info("✅ Updated provider {} rating to {}", providerId, avgRating);
        }
    }

    /**
     * Helper: Convert Rating -> RatingDTO
     */
    private RatingDTO toRatingDTO(Rating rating) {
        RatingDTO dto = RatingDTO.from(rating);

        // Fetch driver info (optional - để driver name hiển thị cho provider)
        orderRepository.findById(rating.getOrderId()).ifPresent(order -> {
            userRepository.findById(order.getDriverId()).ifPresent(user -> {
                dto.setDriverName(user.getFullName());
                dto.setDriverAvatar(user.getAvatarUrl());
            });
        });

        return dto;
    }
}