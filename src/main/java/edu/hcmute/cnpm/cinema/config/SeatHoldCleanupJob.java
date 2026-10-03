package edu.hcmute.cnpm.cinema.config;

import edu.hcmute.cnpm.cinema.service.SeatHoldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tác vụ chạy nền dọn vé giữ quá hạn, trả ghế về trạng thái trống (M2.6).
 *
 * Chạy mỗi 30 giây. Thời gian giữ ghế là 5 phút nên không cần chạy dày hơn;
 * chậm nhất khách phải chờ thêm nửa phút mới thấy ghế trống lại.
 *
 * Tắt được bằng {@code app.seat-hold.cleanup.enabled=false} - profile test tắt
 * tác vụ này để nó không xen vào giữa lúc test đang chạy rồi xoá mất dữ liệu
 * mà test vừa tạo.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.seat-hold.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class SeatHoldCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldCleanupJob.class);

    private final SeatHoldService seatHoldService;

    public SeatHoldCleanupJob(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void releaseExpiredHolds() {
        try {
            seatHoldService.releaseExpiredHolds();
        } catch (RuntimeException exception) {
            // Nuốt lỗi để một lần chạy hỏng không làm Spring dừng luôn lịch chạy.
            log.warn("Don ve giu qua han that bai: {}", exception.getMessage());
        }
    }
}
