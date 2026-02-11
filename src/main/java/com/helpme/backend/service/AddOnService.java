package com.helpme.backend.service;

import com.helpme.backend.dto.AddOnDTO;
import com.helpme.backend.dto.CreateAddOnRequest;
import com.helpme.backend.entity.*;
import com.helpme.backend.exception.BadRequestException;
import com.helpme.backend.exception.ForbiddenException;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.*;
import com.helpme.backend.websocket.SocketIOService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AddOnService {

    private final AddOnRepository addOnRepository;
    private final OrderRepository orderRepository;
    private final LocalStorageService storageService;
private final SocketIOService socketService;  // ✅ Add this
    
    /**
     * Tạo add-on mới (Provider only)
     */
    public AddOnDTO createAddOn(UUID orderId, CreateAddOnRequest request, User providerUser) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        if (order.getProviderId() == null || !order.getProviderId().equals(providerUser.getId())) {
            throw new ForbiddenException("Not your order");
        }
        
        if (order.getStatus() != OrderStatus.IN_SERVICE) {
            throw new BadRequestException("Can only add add-ons during IN_SERVICE status");
        }
        
        AddOn addOn = new AddOn();
        addOn.setOrderId(order.getId());
        addOn.setReasonCode(request.getReasonCode());
        addOn.setReasonText(request.getReasonText());
        addOn.setAmount(request.getAmount());
        addOn.setStatus(AddOnStatus.PENDING);
        
        if (request.getEvidenceImage() != null && !request.getEvidenceImage().isEmpty()) {
            String imagePath = storageService.saveImage(
                request.getEvidenceImage(),
                "addons/" + orderId
            );
            addOn.setEvidenceImage(imagePath);
        }
        
        AddOn savedAddOn = addOnRepository.save(addOn);
        
        log.info("✅ Add-on created: {} for order: {}", savedAddOn.getId(), orderId);
        
        // ✅ Notify driver via Socket.IO
        AddOnDTO addOnDTO = AddOnDTO.from(savedAddOn);
        socketService.emitToUser(
            order.getDriverId(),
            "addon_request",
            addOnDTO
        );
        
        return addOnDTO;
    }

    /**
     * Approve add-on (Driver only)
     */
    public void approveAddOn(UUID orderId, UUID addOnId, User driver) {
        AddOn addOn = addOnRepository.findById(addOnId)
                .orElseThrow(() -> new NotFoundException("Add-on not found"));

        Order order = orderRepository.findById(addOn.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getDriverId().equals(driver.getId())) {
            throw new ForbiddenException("Not your order");
        }

        if (addOn.getStatus() != AddOnStatus.PENDING) {
            throw new BadRequestException("Add-on is not pending");
        }

        addOn.setStatus(AddOnStatus.APPROVED);
        addOn.setApprovedAt(LocalDateTime.now());
        addOnRepository.save(addOn);

        order.setFinalAmount(order.getFinalAmount().add(addOn.getAmount()));
        orderRepository.save(order);

        log.info("✅ Add-on {} approved for order {}", addOnId, orderId);

        // ✅ Notify provider via Socket.IO
        socketService.emitToUser(
                order.getProviderId(),
                "addon_approved",
                Map.of(
                        "addOnId", addOn.getId(),
                        "orderId", order.getId(),
                        "message", "Chi phí phát sinh đã được chấp nhận"));
    }

    /**
     * Reject add-on (Driver only)
     */
    public void rejectAddOn(UUID orderId, UUID addOnId, User driver) {
        AddOn addOn = addOnRepository.findById(addOnId)
                .orElseThrow(() -> new NotFoundException("Add-on not found"));

        Order order = orderRepository.findById(addOn.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getDriverId().equals(driver.getId())) {
            throw new ForbiddenException("Not your order");
        }

        if (addOn.getStatus() != AddOnStatus.PENDING) {
            throw new BadRequestException("Add-on is not pending");
        }

        addOn.setStatus(AddOnStatus.REJECTED);
        addOn.setRejectedAt(LocalDateTime.now());
        addOnRepository.save(addOn);

        log.info("✅ Add-on {} rejected for order {}", addOnId, orderId);

        // ✅ Notify provider via Socket.IO
        socketService.emitToUser(
                order.getProviderId(),
                "addon_rejected",
                Map.of(
                        "addOnId", addOn.getId(),
                        "orderId", order.getId(),
                        "message", "Chi phí phát sinh đã bị từ chối"));
    }

    /**
     * Get all add-ons of an order
     */
    public List<AddOnDTO> getAddOns(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Check permission
        boolean isDriver = order.getDriverId().equals(user.getId());
        boolean isProvider = order.getProviderId() != null &&
                order.getProviderId().equals(user.getId());

        if (!isDriver && !isProvider) {
            throw new ForbiddenException("Access denied");
        }

        List<AddOn> addOns = addOnRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        return addOns.stream()
                .map(AddOnDTO::from)
                .collect(Collectors.toList());
    }
}