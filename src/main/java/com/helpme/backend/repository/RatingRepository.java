package com.helpme.backend.repository;

import com.helpme.backend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    /**
     * Kiểm tra order đã được rate chưa
     */
    Optional<Rating> findByOrderId(UUID orderId);

    /**
     * Lấy tất cả ratings của provider
     */
    List<Rating> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    /**
     * Tính rating trung bình của provider
     */
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.providerId = :providerId")
    Double calculateAverageRating(@Param("providerId") UUID providerId);

    /**
     * Đếm số ratings theo từng mức (1-5 sao)
     */
    @Query("SELECT r.rating, COUNT(r) FROM Rating r " +
            "WHERE r.providerId = :providerId " +
            "GROUP BY r.rating " +
            "ORDER BY r.rating DESC")
    List<Object[]> countRatingsByLevel(@Param("providerId") UUID providerId);

    /**
     * Kiểm tra order đã được rate chưa
     */
    boolean existsByOrderId(UUID orderId);
}