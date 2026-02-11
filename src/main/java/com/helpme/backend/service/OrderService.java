package com.helpme.backend.service;

import com.helpme.backend.dto.CreateOrderRequest;
import com.helpme.backend.dto.OrderDTO;
import com.helpme.backend.dto.QuoteDTO;
import com.helpme.backend.entity.*;
import com.helpme.backend.exception.BadRequestException;
import com.helpme.backend.exception.ForbiddenException;
import com.helpme.backend.exception.NotFoundException;
import com.helpme.backend.repository.*;
import com.helpme.backend.websocket.SocketIOService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final GeometryFactory geometryFactory;
        private final BroadcastService broadcastService;  // ✅ Add this
    private final SocketIOService socketService;      // ✅ Add this
    
    /**
     * Tạo order mới (Driver only)
     */
    public OrderDTO createOrder(CreateOrderRequest request, User driver) {
        if (driver.getRole() != UserRole.DRIVER) {
            throw new ForbiddenException("Only drivers can create orders");
        }
        
        orderRepository.findActiveOrderByDriverId(driver.getId())
            .ifPresent(activeOrder -> {
                throw new BadRequestException(
                    "You already have an active order: " + activeOrder.getId()
                );
            });
        
        Order order = new Order();
        order.setDriverId(driver.getId());
        order.setServiceType(request.getServiceType());
        order.setDescription(request.getDescription());
        order.setMediaUrls(request.getMediaUrls());
        order.setStatus(OrderStatus.BROADCASTING);
        order.setBroadcastRadius(request.getBroadcastRadius());
        order.setFinalAmount(BigDecimal.ZERO);
        
        Point location = geometryFactory.createPoint(
            new Coordinate(request.getLng(), request.getLat())
        );
        location.setSRID(4326);
        order.setPickupLocation(location);
        
        Order savedOrder = orderRepository.save(order);
        
        log.info("✅ Order created: {} by driver: {}", savedOrder.getId(), driver.getId());
        
        // ✅ Broadcast to nearby providers
        broadcastService.notifyNearbyProviders(savedOrder);
        
        return toOrderDTO(savedOrder);
    }

    /**
     * Lấy chi tiết order
     */
    public OrderDTO getOrderDetails(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Check permission
        boolean isDriver = order.getDriverId().equals(user.getId());
        boolean isProvider = order.getProviderId() != null &&
                order.getProviderId().equals(user.getId());

        if (!isDriver && !isProvider) {
            throw new ForbiddenException("Access denied");
        }

        return toOrderDTO(order);
    }

    /**
     * Lấy danh sách quotes của order (Driver only)
     */
    public List<QuoteDTO> getQuotes(UUID orderId, User driver) {
        Order order = orderRepository.findByIdAndDriverId(orderId, driver.getId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        List<Quote> quotes = quoteRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        return quotes.stream()
                .map(this::toQuoteDTO)
                .collect(Collectors.toList());
    }

    /**
     * Accept một quote (Driver only)
     */
    public void acceptQuote(UUID quoteId, User driver) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new NotFoundException("Quote not found"));

        Order order = orderRepository.findById(quote.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getDriverId().equals(driver.getId())) {
            throw new ForbiddenException("Not your order");
        }

        if (order.getStatus() != OrderStatus.BROADCASTING) {
            throw new BadRequestException("Order is not in broadcasting state");
        }

        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new BadRequestException("Quote is not pending");
        }

        quote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(quote);

        quoteRepository.rejectOtherQuotes(order.getId(), quoteId);

        order.setStatus(OrderStatus.MATCHED);
        order.setProviderId(quote.getProviderId());
        order.setFinalAmount(quote.getTotalEst());
        orderRepository.save(order);

        log.info("✅ Quote {} accepted for order {}", quoteId, order.getId());

        // ✅ Notify provider via Socket.IO
        socketService.emitToUser(
                quote.getProviderId(),
                "quote_accepted",
                Map.of(
                        "quoteId", quote.getId(),
                        "orderId", order.getId(),
                        "message", "Your quote has been accepted!"));
    }

    /**
     * Lấy lịch sử orders
     */
    public List<OrderDTO> getOrderHistory(User user) {
        List<Order> orders;

        if (user.getRole() == UserRole.DRIVER) {
            orders = orderRepository.findByDriverIdOrderByCreatedAtDesc(user.getId());
        } else {
            orders = orderRepository.findByProviderIdOrderByCreatedAtDesc(user.getId());
        }

        return orders.stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Convert Order -> OrderDTO with related data
     */
    private OrderDTO toOrderDTO(Order order) {
        OrderDTO dto = OrderDTO.fromBasic(order);

        // Fetch driver info
        if (order.getDriverId() != null) {
            userRepository.findById(order.getDriverId())
                    .ifPresent(user -> {
                        dto.setDriverName(user.getFullName());
                        dto.setDriverPhone(user.getPhone());
                        dto.setDriverAvatar(user.getAvatarUrl());
                    });
        }

        // Fetch provider info
        if (order.getProviderId() != null) {
            providerRepository.findById(order.getProviderId())
                    .ifPresent(provider -> {
                        dto.setProviderOrgName(provider.getOrgName());
                        dto.setProviderRating(provider.getRatingAvg());

                        userRepository.findById(provider.getId())
                                .ifPresent(user -> {
                                    dto.setProviderPhone(user.getPhone());
                                    dto.setProviderAvatar(user.getAvatarUrl());
                                });
                    });
        }

        return dto;
    }

    /**
     * Helper: Convert Quote -> QuoteDTO with provider info
     */
    private QuoteDTO toQuoteDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.fromBasic(quote);

        // Fetch provider info
        providerRepository.findById(quote.getProviderId())
                .ifPresent(provider -> {
                    dto.setProviderOrgName(provider.getOrgName());
                    dto.setProviderRating(provider.getRatingAvg());

                    userRepository.findById(provider.getId())
                            .ifPresent(user -> {
                                dto.setProviderPhone(user.getPhone());
                                dto.setProviderAvatar(user.getAvatarUrl());
                            });
                });

        return dto;
    }
}