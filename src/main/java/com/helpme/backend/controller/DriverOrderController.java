package com.helpme.backend.controller;

import com.helpme.backend.dto.*;
import com.helpme.backend.entity.User;
import com.helpme.backend.service.AddOnService;
import com.helpme.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/driver/orders")
@RequiredArgsConstructor
public class DriverOrderController {

    private final OrderService orderService;
    private final AddOnService addOnService;

    /**
     * POST /v1/driver/orders
     * Tạo order mới
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestBody @Valid CreateOrderRequest request,
            @AuthenticationPrincipal User currentUser) {
        OrderDTO order = orderService.createOrder(request, currentUser);
        return ResponseEntity.ok(order);
    }

    /**
     * GET /v1/driver/orders/{orderId}
     * Lấy chi tiết order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderDetails(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        OrderDTO order = orderService.getOrderDetails(orderId, currentUser);
        return ResponseEntity.ok(order);
    }

    /**
     * GET /v1/driver/orders/{orderId}/quotes
     * Lấy danh sách quotes
     */
    @GetMapping("/{orderId}/quotes")
    public ResponseEntity<List<QuoteDTO>> getQuotes(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        List<QuoteDTO> quotes = orderService.getQuotes(orderId, currentUser);
        return ResponseEntity.ok(quotes);
    }

    /**
     * PATCH /v1/driver/orders/quotes/{quoteId}/accept
     * Chấp nhận một quote
     */
    @PatchMapping("/quotes/{quoteId}/accept")
    public ResponseEntity<MessageResponse> acceptQuote(
            @PathVariable UUID quoteId,
            @AuthenticationPrincipal User currentUser) {
        orderService.acceptQuote(quoteId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Quote accepted successfully"));
    }

    /**
     * GET /v1/driver/orders/history
     * Lấy lịch sử orders
     */
    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getOrderHistory(
            @AuthenticationPrincipal User currentUser) {
        List<OrderDTO> orders = orderService.getOrderHistory(currentUser);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /v1/driver/orders/{orderId}/addons
     * Lấy danh sách add-ons
     */
    @GetMapping("/{orderId}/addons")
    public ResponseEntity<List<AddOnDTO>> getAddOns(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        List<AddOnDTO> addOns = addOnService.getAddOns(orderId, currentUser);
        return ResponseEntity.ok(addOns);
    }

    /**
     * POST /v1/driver/orders/{orderId}/addons/{addOnId}/approve
     * Approve add-on
     */
    @PostMapping("/{orderId}/addons/{addOnId}/approve")
    public ResponseEntity<MessageResponse> approveAddOn(
            @PathVariable UUID orderId,
            @PathVariable UUID addOnId,
            @AuthenticationPrincipal User currentUser) {
        addOnService.approveAddOn(orderId, addOnId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Add-on approved successfully"));
    }

    /**
     * POST /v1/driver/orders/{orderId}/addons/{addOnId}/reject
     * Reject add-on
     */
    @PostMapping("/{orderId}/addons/{addOnId}/reject")
    public ResponseEntity<MessageResponse> rejectAddOn(
            @PathVariable UUID orderId,
            @PathVariable UUID addOnId,
            @AuthenticationPrincipal User currentUser) {
        addOnService.rejectAddOn(orderId, addOnId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Add-on rejected successfully"));
    }
}