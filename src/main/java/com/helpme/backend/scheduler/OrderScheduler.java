package com.helpme.backend.scheduler;

import com.helpme.backend.entity.Order;
import com.helpme.backend.entity.OrderStatus;
import com.helpme.backend.entity.User;
import com.helpme.backend.repository.OrderRepository;
import com.helpme.backend.repository.UserRepository;
import com.helpme.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Chạy mỗi 5 phút để check orders bị "stale" (quá lâu ở trạng thái
     * BROADCASTING)
     * Gửi thông báo cho driver để mở rộng bán kính hoặc thử lại
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 5 * 60 * 1000
    public void checkStaleOrders() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);

        List<Order> staleOrders = orderRepository.findStaleOrders(
                OrderStatus.BROADCASTING,
                cutoffTime);

        if (!staleOrders.isEmpty()) {
            log.info("⏰ Found {} stale orders", staleOrders.size());

            staleOrders.forEach(order -> {
                // Notify driver
                userRepository.findById(order.getDriverId()).ifPresent(driver -> {
                    if (driver.getPushToken() != null) {
                        notificationService.sendPushNotification(
                                driver.getPushToken(),
                                "Không tìm thấy thợ cứu hộ",
                                "Đơn hàng của bạn chưa có ai nhận. Vui lòng thử lại hoặc mở rộng bán kính tìm kiếm.");

                        log.info("📲 Notified driver {} about stale order {}",
                                driver.getId(), order.getId());
                    }
                });
            });
        }
    }

    /**
     * Chạy mỗi ngày lúc 2:00 AM để clean up orders cũ
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldOrders() {
        // TODO: Archive hoặc xóa orders đã COMPLETED/CANCELLED quá 30 ngày
        log.info("🧹 Running cleanup task for old orders");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Example: Có thể move sang bảng archive hoặc soft delete
        // orderRepository.archiveOldOrders(thirtyDaysAgo);
    }

    /**
     * Chạy mỗi 1 phút để check orders đang IN_SERVICE quá lâu
     * Có thể là provider quên update status
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void checkLongRunningServiceOrders() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

        List<Order> longRunningOrders = orderRepository.findStaleOrders(
                OrderStatus.IN_SERVICE,
                twoHoursAgo);

        if (!longRunningOrders.isEmpty()) {
            log.warn("⚠️ Found {} orders in IN_SERVICE for over 2 hours",
                    longRunningOrders.size());

            // TODO: Có thể gửi alert cho admin hoặc notify driver
        }
    }
}