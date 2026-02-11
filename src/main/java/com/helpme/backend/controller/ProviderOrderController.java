package com.helpme.backend.controller;

import com.helpme.backend.dto.*;
import com.helpme.backend.entity.Provider;
import com.helpme.backend.entity.User;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.ProviderRepository;
import com.helpme.backend.repository.UserRepository;
import com.helpme.backend.service.AddOnService;
import com.helpme.backend.service.ProviderService;
import com.helpme.backend.service.QuoteService;
import com.helpme.backend.service.RedisLocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/provider")
@RequiredArgsConstructor
public class ProviderOrderController {

    private final ProviderService providerService;
    private final QuoteService quoteService;
    private final AddOnService addOnService;
    private final RedisLocationService redisLocationService;  // ✅ Add this
    private final ProviderRepository providerRepository;  // ✅ Add this
    private final UserRepository userRepository;          // ✅ Add this

    /**
     * PATCH /v1/provider/online-status
     * Update online/offline status
     */
    @PatchMapping("/online-status")
    public ResponseEntity<MessageResponse> updateOnlineStatus(
            @RequestParam boolean isOnline,
            @AuthenticationPrincipal User currentUser) {
        providerService.updateOnlineStatus(currentUser, isOnline);
        return ResponseEntity.ok(new MessageResponse(
                "Provider is now " + (isOnline ? "ONLINE" : "OFFLINE")));
    }

    /**
     * GET /v1/provider/orders/nearby
     * Tìm orders gần provider
     */
    @GetMapping("/orders/nearby")
    public ResponseEntity<List<OrderDTO>> getNearbyOrders(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10000") int radius,
            @AuthenticationPrincipal User currentUser) {
        List<OrderDTO> orders = providerService.findNearbyOrders(lat, lng, radius, currentUser);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /v1/provider/quotes
     * Gửi quote
     */
    @PostMapping("/quotes")
    public ResponseEntity<QuoteDTO> sendQuote(
            @RequestBody @Valid CreateQuoteRequest request,
            @AuthenticationPrincipal User currentUser) {
        QuoteDTO quote = quoteService.createQuote(request, currentUser);
        return ResponseEntity.ok(quote);
    }

    /**
     * PATCH /v1/provider/orders/{orderId}/status
     * Update order status
     */
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<MessageResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody @Valid UpdateOrderStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        providerService.updateOrderStatus(orderId, request.getStatus(), currentUser);
        return ResponseEntity.ok(new MessageResponse("Order status updated successfully"));
    }

    /**
     * POST /v1/provider/orders/{orderId}/addons
     * Tạo add-on
     */
    @PostMapping("/orders/{orderId}/addons")
    public ResponseEntity<AddOnDTO> createAddOn(
            @PathVariable UUID orderId,
            @RequestParam String reasonCode,
            @RequestParam(required = false) String reasonText,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) MultipartFile evidenceImage,
            @AuthenticationPrincipal User currentUser) {
        CreateAddOnRequest request = new CreateAddOnRequest();
        request.setReasonCode(reasonCode);
        request.setReasonText(reasonText);
        request.setAmount(amount);
        request.setEvidenceImage(evidenceImage);

        AddOnDTO addOn = addOnService.createAddOn(orderId, request, currentUser);
        return ResponseEntity.ok(addOn);
    }

    @PostMapping("/location")
    public ResponseEntity<MessageResponse> updateLiveLocation(
            @RequestParam double lat,
            @RequestParam double lng,
            @AuthenticationPrincipal User currentUser) {
        providerService.updateLiveLocation(currentUser, lat, lng);
        return ResponseEntity.ok(new MessageResponse("Location updated successfully"));
    }

    /**
     * GET /v1/provider/profile
     * Lấy thông tin profile của provider
     */
    @GetMapping("/profile")
    public ResponseEntity<ProviderDTO> getProfile(
            @AuthenticationPrincipal User currentUser) {
        Provider provider = providerRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        ProviderDTO dto = ProviderDTO.from(provider);
        dto.setPhone(currentUser.getPhone());
        dto.setAvatarUrl(currentUser.getAvatarUrl());

        return ResponseEntity.ok(dto);
    }
}