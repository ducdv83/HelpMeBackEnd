package com.helpme.backend.repository;

import com.helpme.backend.entity.Quote;
import com.helpme.backend.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    /**
     * Lấy tất cả quotes của một order (sắp xếp theo thời gian tạo)
     */
    List<Quote> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    /**
     * Lấy quotes theo provider
     */
    List<Quote> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    /**
     * Kiểm tra provider đã gửi quote cho order này chưa
     */
    boolean existsByOrderIdAndProviderId(UUID orderId, UUID providerId);

    /**
     * Lấy quote theo ID và kiểm tra ownership
     */
    Optional<Quote> findByIdAndProviderId(UUID quoteId, UUID providerId);

    /**
     * Lấy quote đã được ACCEPTED của một order
     */
    Optional<Quote> findByOrderIdAndStatus(UUID orderId, QuoteStatus status);

    /**
     * Reject tất cả quotes khác khi driver accept 1 quote
     */
    @Modifying
    @Query("UPDATE Quote q SET q.status = 'REJECTED' " +
            "WHERE q.orderId = :orderId AND q.id != :acceptedQuoteId " +
            "AND q.status = 'PENDING'")
    void rejectOtherQuotes(
            @Param("orderId") UUID orderId,
            @Param("acceptedQuoteId") UUID acceptedQuoteId);

    /**
     * Đếm số quotes pending của một order
     */
    long countByOrderIdAndStatus(UUID orderId, QuoteStatus status);
}