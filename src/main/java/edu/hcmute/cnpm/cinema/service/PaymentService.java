package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {
    private final TicketRepository ticketRepository;
    private final BookingOrderService bookingOrderService;
    private final BookingLockService locks;
    private final BookingClock clock;
    public PaymentService(TicketRepository tickets, BookingOrderService orders, BookingLockService locks, BookingClock clock) {
        ticketRepository = tickets; bookingOrderService = orders; this.locks = locks; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<Ticket> findPayableTickets(Long userId, Long showtimeId) {
        return ticketRepository.findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD)
                .stream().filter(ticket -> !clock.expired(ticket)
                        && ticket.getShowtime().getStartTime().isAfter(clock.now())).toList();
    }
    @Transactional
    public List<Ticket> confirmPayment(Long userId, Long showtimeId) {
        return confirmPayment(userId, showtimeId, PaymentMethod.COUNTER, null);
    }
    @Transactional
    public List<Ticket> confirmPayment(Long userId, Long showtimeId, PaymentMethod method, String ref) {
        return confirmPayment(userId, showtimeId, method, ref, null, null, null);
    }
    @Transactional
    public List<Ticket> confirmPayment(Long userId, Long showtimeId, PaymentMethod method, String ref,
                                      List<Long> expectedIds, Long anchorId, Long expectedAmount) {
        Showtime showtime = locks.lock(showtimeId);
        if (showtime.getStartTime() == null || !showtime.getStartTime().isAfter(clock.now())
                || !Boolean.TRUE.equals(showtime.getMovie().getActive()))
            throw new InvalidBookingException("Suất chiếu không còn nhận thanh toán.");
        if (ref != null && !ticketRepository.findByPaymentRef(ref).isEmpty())
            throw new InvalidBookingException("Giao dịch đã được xác nhận.");
        List<Ticket> held = ticketRepository.findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);
        if (held.isEmpty()) throw new InvalidBookingException("Không tìm thấy vé đang giữ nào cho suất chiếu này. Bạn hãy chọn ghế lại.");
        if (expectedIds != null) HoldIdentity.requireMatch(expectedIds, held.stream().map(Ticket::getId).toList());
        if (anchorId != null && !anchorId.equals(held.stream().map(Ticket::getId).min(Long::compareTo).orElseThrow()))
            throw new InvalidBookingException("Giao dịch thuộc lượt giữ ghế cũ. Không thể thanh toán lượt giữ mới.");
        if (held.stream().anyMatch(clock::expired))
            throw new InvalidBookingException("Đã quá " + Constants.SEAT_HOLD_MINUTES + " phút giữ ghế. Vui lòng chọn lại.");
        if (expectedAmount != null && bookingOrderService.prepareForPayment(userId, showtimeId)
                .getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValueExact() != expectedAmount)
            throw new InvalidBookingException("Số tiền thanh toán không khớp với đơn hàng hiện tại.");
        LocalDateTime paidAt = clock.now();
        bookingOrderService.completeOrder(userId, showtimeId, held, method, ref, paidAt);
        for (Ticket ticket : held) {
            ticket.setStatus(TicketStatus.PAID); ticket.setPaidAt(paidAt);
            ticket.setPaymentMethod(method); ticket.setPaymentRef(ref);
        }
        return ticketRepository.saveAll(held);
    }
    @Transactional
    public List<Ticket> confirmCounterPayment(Long userId, Long showtimeId, List<Long> expectedIds) {
        if (expectedIds == null) throw new InvalidBookingException("Thiếu mã lượt giữ ghế. Vui lòng tải lại trang.");
        return confirmPayment(userId, showtimeId, PaymentMethod.COUNTER, null, expectedIds, null, null);
    }
    public record Checkout(List<Ticket> tickets, BigDecimal total) {}
    @Transactional
    public Checkout prepareCheckout(Long userId, Long showtimeId, List<Long> expectedIds) {
        locks.lock(showtimeId);
        List<Ticket> payable = findPayableTickets(userId, showtimeId);
        if (payable.isEmpty()) throw new InvalidBookingException("Không còn ghế nào đang giữ. Vui lòng chọn lại.");
        if (expectedIds != null) HoldIdentity.requireMatch(expectedIds, payable.stream().map(Ticket::getId).toList());
        return new Checkout(List.copyOf(payable), bookingOrderService.prepareForPayment(userId, showtimeId).getTotalAmount());
    }
    @Transactional
    public BigDecimal totalDue(Long userId, Long showtimeId) {
        return bookingOrderService.prepareForPayment(userId, showtimeId).getTotalAmount();
    }
    @Transactional(readOnly = true)
    public List<Ticket> findTicketHistory(Long userId) { return ticketRepository.findByUserIdOrderByHeldAtDesc(userId); }
    public BigDecimal sumPrice(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::getPrice).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
