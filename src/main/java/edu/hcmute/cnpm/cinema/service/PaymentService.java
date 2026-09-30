package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.PaymentMethod;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Xác nhận thanh toán: chuyển vé từ trạng thái đang giữ sang đã thanh toán.
 *
 * Có hai đường vào: trả tại quầy (bấm xác nhận trên trang) và trả qua MoMo
 * ({@link MomoPaymentService} gọi vào đây sau khi MoMo báo đã nhận tiền).
 */
@Service
public class PaymentService {

    private final TicketRepository ticketRepository;
    private final BookingOrderService bookingOrderService;

    public PaymentService(TicketRepository ticketRepository, BookingOrderService bookingOrderService) {
        this.ticketRepository = ticketRepository;
        this.bookingOrderService = bookingOrderService;
    }

    /**
     * Các vé đang giữ của khách cho một suất chiếu, còn trong thời gian giữ ghế.
     *
     * Vé giữ quá {@link Constants#SEAT_HOLD_MINUTES} phút coi như đã mất chỗ, không
     * cho thanh toán nữa - nếu không thì khách mở trang cũ rồi bấm trả tiền cho
     * một ghế mà người khác đã lấy mất.
     */
    @Transactional(readOnly = true)
    public List<Ticket> findPayableTickets(Long userId, Long showtimeId) {
        return ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD)
                .stream()
                .filter(ticket -> !isExpired(ticket))
                .toList();
    }

    /**
     * Xác nhận thanh toán cho toàn bộ vé đang giữ của khách ở suất chiếu này.
     *
     * @return danh sách vé đã chuyển sang đã thanh toán
     */
    @Transactional
    public List<Ticket> confirmPayment(Long userId, Long showtimeId) {
        return confirmPayment(userId, showtimeId, PaymentMethod.COUNTER, null);
    }

    /**
     * Xác nhận thanh toán, ghi kèm cách trả và mã giao dịch bên cổng thanh toán.
     *
     * @param paymentRef mã giao dịch của cổng thanh toán (transId của MoMo), null nếu trả tại quầy
     */
    @Transactional
    public List<Ticket> confirmPayment(Long userId, Long showtimeId, PaymentMethod paymentMethod, String paymentRef) {
        List<Ticket> held = ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);

        if (held.isEmpty()) {
            throw new InvalidBookingException(
                    "Không tìm thấy vé đang giữ nào cho suất chiếu này. Bạn hãy chọn ghế lại.");
        }
        for (Ticket ticket : held) {
            if (isExpired(ticket)) {
                throw new InvalidBookingException("Đã quá " + Constants.SEAT_HOLD_MINUTES
                        + " phút giữ ghế nên vé không còn hiệu lực. Bạn hãy chọn ghế lại.");
            }
        }

        LocalDateTime paidAt = LocalDateTime.now();
        // Chốt đơn trước khi lưu vé để cùng một transaction chứa cả vé, combo và hóa đơn.
        bookingOrderService.completeOrder(userId, showtimeId, held, paymentMethod, paymentRef, paidAt);
        for (Ticket ticket : held) {
            ticket.setStatus(TicketStatus.PAID);
            ticket.setPaidAt(paidAt);
            ticket.setPaymentMethod(paymentMethod);
            ticket.setPaymentRef(paymentRef);
        }
        return ticketRepository.saveAll(held);
    }

    /** Tổng cần trả của cả đơn, gồm vé và bắp nước. */
    @Transactional
    public BigDecimal totalDue(Long userId, Long showtimeId) {
        return bookingOrderService.prepareForPayment(userId, showtimeId).getTotalAmount();
    }

    /** Toàn bộ vé của một khách, mới nhất lên đầu. */
    @Transactional(readOnly = true)
    public List<Ticket> findTicketHistory(Long userId) {
        return ticketRepository.findByUserIdOrderByHeldAtDesc(userId);
    }

    /** Cộng tiền của một danh sách vé. */
    public BigDecimal sumPrice(List<Ticket> tickets) {
        BigDecimal total = BigDecimal.ZERO;
        for (Ticket ticket : tickets) {
            if (ticket.getPrice() != null) {
                total = total.add(ticket.getPrice());
            }
        }
        return total;
    }

    private boolean isExpired(Ticket ticket) {
        return ticket.getHeldAt() == null
                || ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES)
                        .isBefore(LocalDateTime.now());
    }
}
