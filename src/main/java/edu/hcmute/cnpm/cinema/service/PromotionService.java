package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.util.MoneyFormatter;
import edu.hcmute.cnpm.cinema.entity.Voucher;
import edu.hcmute.cnpm.cinema.repository.VoucherRepository;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PromotionService {
    private final VoucherRepository vouchers;
    private final BookingClock clock;

    public PromotionService(VoucherRepository vouchers, BookingClock clock) {
        this.vouchers = vouchers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Voucher> availableOffers() {
        return vouchers.findAllByOrderByCodeAsc().stream().filter(this::available).toList();
    }

    private boolean available(Voucher offer) {
        var today = clock.now().toLocalDate();
        return offer.isActive() && (offer.getStartsOn() == null || !today.isBefore(offer.getStartsOn()))
                && (offer.getEndsOn() == null || !today.isAfter(offer.getEndsOn()));
    }

    public String normalize(String code) {
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public BigDecimal discount(String code, BigDecimal ticketSubtotal) {
        Voucher offer = vouchers.findByCode(normalize(code))
                .orElseThrow(() -> new BusinessException("Mã ưu đãi không tồn tại."));
        if (!available(offer)) throw new BusinessException("Mã ưu đãi chưa có hiệu lực, đã hết hạn hoặc đã ngừng áp dụng.");
        if (ticketSubtotal.compareTo(offer.getMinimum()) < 0)
            throw new BusinessException("Mã " + offer.getCode() + " yêu cầu tiền vé tối thiểu "
                    + MoneyFormatter.format(offer.getMinimum()) + " (không tính bắp nước).");
        BigDecimal reduction = (offer.getPercent() > 0
                ? ticketSubtotal.multiply(BigDecimal.valueOf(offer.getPercent())).divide(BigDecimal.valueOf(100))
                : offer.getAmount()).min(offer.getMaximum()).setScale(0, RoundingMode.DOWN);
        if (reduction.signum() <= 0 || reduction.compareTo(ticketSubtotal) >= 0)
            throw new BusinessException("Giá trị đơn vé không đủ để áp dụng mã ưu đãi này.");
        return reduction;
    }

    /** Đơn nháp được tính lại khi đổi ghế/bắp nước; đơn đã trả giữ nguyên giá trị đã chốt. */
    public void recalculate(BookingOrder order) {
        BigDecimal reduction = BigDecimal.ZERO;
        if (order.getVoucherCode() != null) {
            try { reduction = discount(order.getVoucherCode(), order.getTicketSubtotal()); }
            catch (BusinessException invalid) { order.setVoucherCode(null); }
        }
        order.setDiscountAmount(reduction);
        order.setTotalAmount(order.getTicketSubtotal().add(order.getConcessionSubtotal()).subtract(reduction));
    }

    /** Phân bổ giảm giá theo giá vé, làm tròn theo phần cộng dồn để không mất đồng nào.
     * Giá gốc được giữ lại cho hóa đơn; giá thực trả dùng cho hoàn tiền và doanh thu. */
    public void allocateDiscount(BookingOrder order, List<Ticket> tickets) {
        if (order.getDiscountAmount().signum() == 0) return;
        List<Ticket> sorted = tickets.stream().sorted(Comparator.comparing(Ticket::getId)).toList();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal allocated = BigDecimal.ZERO;
        for (Ticket ticket : sorted) {
            BigDecimal original = ticket.getPrice();
            cumulative = cumulative.add(original);
            BigDecimal next = order.getDiscountAmount().multiply(cumulative)
                    .divide(order.getTicketSubtotal(), 0, RoundingMode.DOWN);
            ticket.setOriginalPrice(original);
            ticket.setPrice(original.subtract(next.subtract(allocated)));
            allocated = next;
        }
    }
}
