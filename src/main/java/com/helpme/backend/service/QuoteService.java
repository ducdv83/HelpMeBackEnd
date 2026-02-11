package com.helpme.backend.service;

import com.helpme.backend.dto.CreateQuoteRequest;
import com.helpme.backend.dto.QuoteDTO;
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

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final OrderRepository orderRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
private final SocketIOService socketService;  // ✅ Add this
    
    /**
     * Tạo quote mới (Provider only)
     */
    public QuoteDTO createQuote(CreateQuoteRequest request, User providerUser) {
        if (providerUser.getRole() != UserRole.PROVIDER) {
            throw new ForbiddenException("Only providers can send quotes");
        }
        
        Provider provider = providerRepository.findById(providerUser.getId())
            .orElseThrow(() -> new NotFoundException("Provider not found"));
        
        if (!provider.getIsOnline()) {
            throw new BadRequestException("Provider must be online to send quotes");
        }
        
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.BROADCASTING) {
            throw new BadRequestException("Order is no longer accepting quotes");
        }
        
        if (quoteRepository.existsByOrderIdAndProviderId(order.getId(), provider.getId())) {
            throw new BadRequestException("You have already sent a quote for this order");
        }
        
        Quote quote = new Quote();
        quote.setOrderId(order.getId());
        quote.setProviderId(provider.getId());
        quote.setStatus(QuoteStatus.PENDING);
        quote.setBasePrice(request.getBasePrice());
        quote.setDistancePrice(request.getDistancePrice() != null ? request.getDistancePrice() : BigDecimal.ZERO);
        quote.setMaterialPrice(request.getMaterialPrice() != null ? request.getMaterialPrice() : BigDecimal.ZERO);
        
        BigDecimal total = request.getBasePrice()
            .add(quote.getDistancePrice())
            .add(quote.getMaterialPrice());
        quote.setTotalEst(total);
        
        quote.setEtaMinutes(request.getEtaMinutes());
        quote.setLocationSource(request.getLocationSource());
        quote.setNotes(request.getNotes());
        
        Quote savedQuote = quoteRepository.save(quote);
        
        log.info("✅ Quote created: {} for order: {} by provider: {}", 
                 savedQuote.getId(), order.getId(), provider.getId());
        
        // ✅ Notify driver via Socket.IO
        QuoteDTO quoteDTO = toQuoteDTO(savedQuote);
        socketService.emitToUser(
            order.getDriverId(),
            "new_quote_received",
            quoteDTO
        );
        
        return quoteDTO;
    }

    /**
     * Helper: Convert Quote -> QuoteDTO
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