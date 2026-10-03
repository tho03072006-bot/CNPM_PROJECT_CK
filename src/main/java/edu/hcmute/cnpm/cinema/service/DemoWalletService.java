package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import edu.hcmute.cnpm.cinema.config.DemoWalletCompatibility;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.payment.DemoWalletRequest;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.*;
import edu.hcmute.cnpm.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** © Nhóm 8. Backend xác nhận và xuất vé mô phỏng trong cùng giao dịch. */
@Service
public class DemoWalletService {
    private final DemoWalletSettings settings;
    private final DemoWalletSecurity security;
    private final DemoPaymentRepository payments;
    private final TicketRepository tickets;
    private final BookingLockService locks;
    private final BookingClock clock;
    private final PaymentService paymentService;
    private final BookingOrderService orders;
    private final DemoWalletCompatibility compatibility;
    public DemoWalletService(DemoWalletSettings settings, DemoWalletSecurity security,
            DemoPaymentRepository payments, TicketRepository tickets, BookingLockService locks,
            BookingClock clock, PaymentService paymentService, BookingOrderService orders,
            DemoWalletCompatibility compatibility) {
        this.settings=settings;this.security=security;this.payments=payments;this.tickets=tickets;
        this.locks=locks;this.clock=clock;this.paymentService=paymentService;this.orders=orders;
        this.compatibility=compatibility;
    }
    public record Issued(String publicId, String token, Long showtimeId) implements java.io.Serializable {}
    public record View(String publicId, String status, String movieTitle, String roomName,
            LocalDateTime showtimeStart, List<String> seatLabels, long amount,
            long expiresAtMillis, long serverTimeMillis, String message) {}
    public record Merchant(Long showtimeId, View payment, String walletUrl, boolean localOnly) {}

    @Transactional
    public Issued create(Long userId, Long showtimeId, List<Long> expectedIds, Issued previous) {
        settings.requireEnabled(); settings.getPublicBaseUrl();
        requireCustomer(userId); HoldIdentity.requireMatch(expectedIds, expectedIds == null ? List.of() : expectedIds);
        compatibility.requireCompatible();
        locks.lock(showtimeId);
        PaymentService.Checkout checkout = paymentService.prepareCheckout(userId, showtimeId, expectedIds);
        if (!Boolean.TRUE.equals(checkout.tickets().getFirst().getShowtime().getMovie().getActive()))
            throw new BusinessException("Phim không còn nhận đặt vé.");
        long amount = wholeAmount(checkout);
        List<Long> ids = checkout.tickets().stream().map(Ticket::getId).sorted().toList();
        if (previous != null && showtimeId.equals(previous.showtimeId())) {
            DemoPayment existing = payments.findByPublicId(previous.publicId()).orElse(null);
            if (existing != null && userId.equals(existing.getUserId())
                    && existing.getStatus() == DemoPaymentStatus.PENDING
                    && clock.now().isBefore(existing.getExpiresAt())
                    && existing.getAmount() == amount && ticketIds(existing).equals(ids)) {
                security.requireToken(previous.token(), existing.getTokenHash()); return previous;
            }
        }
        if (payments.countByUserIdAndCreatedAtAfter(userId, clock.now().minusMinutes(1)) >= 10)
            throw new BusinessException("Bạn đã tạo nhiều mã thanh toán. Vui lòng đợi một phút rồi thử lại.");
        for (DemoPayment old : payments.findByUserIdAndShowtimeIdAndStatus(userId,showtimeId,DemoPaymentStatus.PENDING))
            old.setStatus(DemoPaymentStatus.INVALIDATED);
        String token = security.newToken();
        DemoPayment payment = new DemoPayment();
        payment.setPublicId(UUID.randomUUID().toString());payment.setTokenHash(security.hash(token));
        payment.setUserId(userId);payment.setShowtimeId(showtimeId);
        payment.setTicketIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        payment.setSeatLabels(checkout.tickets().stream()
                .sorted(Comparator.comparing((Ticket t)->t.getSeat().getSeatRow()).thenComparing(t->t.getSeat().getSeatColumn()))
                .map(t->t.getSeat().getSeatRow()+t.getSeat().getSeatColumn()).collect(Collectors.joining(", ")));
        Showtime showtime=checkout.tickets().getFirst().getShowtime();
        payment.setMovieTitle(showtime.getMovie().getTitle());payment.setRoomName(showtime.getRoom().getName());
        payment.setShowtimeStart(showtime.getStartTime());payment.setAmount(amount);payment.setCreatedAt(clock.now());
        LocalDateTime expiry=checkout.tickets().stream().map(t->t.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES))
                .min(LocalDateTime::compareTo).orElseThrow();
        payment.setExpiresAt(expiry.isBefore(showtime.getStartTime()) ? expiry : showtime.getStartTime());
        payments.saveAndFlush(payment);
        return new Issued(payment.getPublicId(),token,showtimeId);
    }
    @Transactional
    public Merchant merchant(String publicId, Long userId, Issued issued) {
        DemoPayment payment=locked(publicId);requireOwner(payment,userId);reconcile(payment);
        String url=null;
        if (issued != null && publicId.equals(issued.publicId()) && payment.getStatus()==DemoPaymentStatus.PENDING) {
            security.requireToken(issued.token(),payment.getTokenHash());
            url=settings.getPublicBaseUrl()+"/demo-wallet/pay/"+publicId+"#token="+issued.token();
        }
        return new Merchant(payment.getShowtimeId(),view(payment),url,settings.isLocalOnly());
    }
    @Transactional
    public View wallet(String publicId, String token) {
        DemoPayment payment=locked(publicId);security.requireToken(token,payment.getTokenHash());
        reconcile(payment);return view(payment);
    }
    @Transactional
    public View confirm(String publicId, DemoWalletRequest request) {
        DemoPayment payment=locked(publicId);
        if (request==null) throw new BusinessException("Thiếu thông tin xác nhận thanh toán.");
        security.requireToken(request.token(),payment.getTokenHash());
        if (!Boolean.TRUE.equals(request.confirmed()))
            throw new BusinessException("Bạn cần xác nhận đây là giao dịch mô phỏng, không phát sinh tiền thật.");
        if (request.expectedAmount()==null || request.expectedAmount()<=0
                || !request.expectedAmount().equals(payment.getAmount()))
            throw new BusinessException("Số tiền xác nhận không khớp. Vui lòng tải lại thông tin giao dịch.");
        reconcile(payment);
        if (payment.getStatus()!=DemoPaymentStatus.PENDING) return view(payment);
        paymentService.confirmPayment(payment.getUserId(),payment.getShowtimeId(),PaymentMethod.MOMO_DEMO,
                reference(payment),ticketIds(payment),ticketIds(payment).getFirst(),payment.getAmount());
        payment.setStatus(DemoPaymentStatus.SUCCESS);payment.setPaidAt(clock.now());
        payments.saveAndFlush(payment); return view(payment);
    }
    @Transactional
    public View cancel(String publicId, String token) {
        DemoPayment payment=locked(publicId);security.requireToken(token,payment.getTokenHash());reconcile(payment);
        if (payment.getStatus()==DemoPaymentStatus.PENDING) payment.setStatus(DemoPaymentStatus.CANCELLED);
        return view(payment);
    }
    @Transactional
    public View cancelOwned(String publicId, Long userId) {
        DemoPayment payment=locked(publicId);requireOwner(payment,userId);reconcile(payment);
        if (payment.getStatus()==DemoPaymentStatus.PENDING) payment.setStatus(DemoPaymentStatus.CANCELLED);
        return view(payment);
    }
    @Transactional
    public List<Long> paidTicketIds(String publicId, Long userId) {
        DemoPayment payment=locked(publicId);requireOwner(payment,userId);
        if (payment.getStatus()!=DemoPaymentStatus.SUCCESS)
            throw new BusinessException("Giao dịch chưa được xác nhận thanh toán. Vui lòng kiểm tra trạng thái.");
        return tickets.findByPaymentRef(reference(payment)).stream().filter(t->userId.equals(t.getUser().getId()))
                .map(Ticket::getId).toList();
    }
    /** Khoá suất trước giao dịch ví, cùng thứ tự với giữ/huỷ/thanh toán khác. */
    private DemoPayment locked(String publicId) {
        settings.requireEnabled();
        if (publicId==null || !publicId.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
            throw new BusinessException("Mã giao dịch không hợp lệ.");
        Long showtimeId=payments.findShowtimeId(publicId)
                .orElseThrow(()->new ResourceNotFoundException("Giao dịch mô phỏng không tồn tại."));
        locks.lock(showtimeId);
        return payments.findForUpdate(publicId).orElseThrow(()->new ResourceNotFoundException("Giao dịch mô phỏng không tồn tại."));
    }
    private void reconcile(DemoPayment payment) {
        if (payment.getStatus()!=DemoPaymentStatus.PENDING) return;
        if (!clock.now().isBefore(payment.getExpiresAt())) {payment.setStatus(DemoPaymentStatus.EXPIRED);return;}
        Showtime showtime=locks.lock(payment.getShowtimeId());
        List<Ticket> held=tickets.findByUserIdAndShowtimeIdAndStatus(payment.getUserId(),payment.getShowtimeId(),TicketStatus.HELD);
        if (!Boolean.TRUE.equals(showtime.getMovie().getActive()) || !showtime.getStartTime().isAfter(clock.now())
                || held.size()!=ticketIds(payment).size()
                || !held.stream().map(Ticket::getId).sorted().toList().equals(ticketIds(payment))
                || held.stream().anyMatch(clock::expired)) {
            payment.setStatus(DemoPaymentStatus.INVALIDATED);return;
        }
        try {
            long currentAmount=orders.prepareForPayment(payment.getUserId(),payment.getShowtimeId())
                    .getTotalAmount().setScale(0,RoundingMode.UNNECESSARY).longValueExact();
            if (currentAmount!=payment.getAmount()) payment.setStatus(DemoPaymentStatus.INVALIDATED);
        } catch (ArithmeticException exception) {
            payment.setStatus(DemoPaymentStatus.INVALIDATED);
        }
    }
    private View view(DemoPayment payment) {
        String message=switch(payment.getStatus()) {
            case PENDING->"Đang chờ xác nhận trên ví MoMo giả lập Nhóm 8.";
            case SUCCESS->"Thanh toán mô phỏng thành công. Không phát sinh tiền thật.";
            case CANCELLED->"Giao dịch mô phỏng đã huỷ. Ghế vẫn theo thời hạn giữ ban đầu.";
            case EXPIRED->"Giao dịch đã hết hạn. Vui lòng quay lại web rạp và chọn ghế.";
            case INVALIDATED->"Mã này không còn phù hợp với lượt giữ hoặc đơn hàng. Hãy tạo mã mới trên web rạp.";
        };
        return new View(payment.getPublicId(),payment.getStatus().name(),payment.getMovieTitle(),payment.getRoomName(),
                payment.getShowtimeStart(),List.of(payment.getSeatLabels().split(", ")),payment.getAmount(),
                clock.epochMillis(payment.getExpiresAt()),clock.millis(),message);
    }
    private long wholeAmount(PaymentService.Checkout checkout) {
        try {
            long amount=checkout.total().setScale(0,RoundingMode.UNNECESSARY).longValueExact();
            if(amount<=0||amount>1_000_000_000L) throw new ArithmeticException();
            return amount;
        } catch (ArithmeticException exception) {throw new BusinessException("Tổng tiền phải là số đồng nguyên dương, tối đa một tỷ đồng.");}
    }
    private List<Long> ticketIds(DemoPayment payment) {
        return Arrays.stream(payment.getTicketIds().split(",")).map(Long::valueOf).sorted().toList();
    }
    private String reference(DemoPayment payment) {return "DEMO-"+payment.getPublicId();}
    private void requireCustomer(Long userId) {
        if(userId==null||userId<=0)throw new BusinessException("Bạn cần đăng nhập để tạo giao dịch.");
    }
    private void requireOwner(DemoPayment payment,Long userId) {
        requireCustomer(userId);
        if(!userId.equals(payment.getUserId()))throw new ResourceNotFoundException("Giao dịch mô phỏng không tồn tại.");
    }
}
