package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.config.MomoProperties;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.payment.MomoCheckout;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult.Outcome;
import edu.hcmute.cnpm.cinema.dto.payment.MomoQrPayment;
import edu.hcmute.cnpm.cinema.dto.payment.MomoQueryResult;
import edu.hcmute.cnpm.cinema.entity.PaymentMethod;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thanh toán vé qua ví MoMo.
 *
 * Luồng: khách bấm "Thanh toán bằng MoMo" → {@link #startPayment} tạo giao dịch và trả về
 * trang của MoMo → khách trả tiền bên đó → MoMo đưa khách quay về kèm kết quả có chữ ký →
 * {@link #handleResult} kiểm chữ ký rồi mới chuyển vé sang đã thanh toán.
 *
 * Luồng quét mã QR (ưu tiên trên trang thanh toán): {@link #startQrPayment} tạo giao dịch
 * kiểu captureWallet, trang của rạp tự vẽ mã QR → khách quét bằng app MoMo trên điện thoại →
 * trang hỏi {@link #checkQrPayment} vài giây một lần, MoMo báo đã trả thì xuất vé.
 *
 * Mọi quyết định dựa trên kết quả đã kiểm chữ ký, không tin gì từ trình duyệt: ai cũng
 * gõ được địa chỉ trang kết quả với resultCode=0, nhưng không ai ký giả được nếu không có
 * khoá bí mật.
 */
@Service
public class MomoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentService.class);
    /** Mã đơn gửi MoMo: UTE-<suất chiếu>-<khách>-<thời điểm>. Kẹp sẵn hai mã để lúc quay về biết vé của ai. */
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^UTE-(\\d+)-(\\d+)-(\\d+)$");
    /**
     * Mã MoMo trả về khi giao dịch chưa xong: 1000 là đã tạo, đang chờ khách xác nhận; 7000 và
     * 7002 là đang xử lý; 9000 là đã được xác nhận, chờ trừ tiền. Mã khác là thất bại hẳn.
     */
    private static final Set<Integer> PENDING_RESULT_CODES = Set.of(1000, 7000, 7002, 9000);

    private final MomoProperties properties;
    private final MomoApiClient apiClient;
    private final PaymentService paymentService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMailService ticketMailService;

    public MomoPaymentService(MomoProperties properties, MomoApiClient apiClient,
                              PaymentService paymentService, TicketRepository ticketRepository,
                              UserRepository userRepository, TicketMailService ticketMailService) {
        this.properties = properties;
        this.apiClient = apiClient;
        this.paymentService = paymentService;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketMailService = ticketMailService;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Tạo giao dịch MoMo cho các ghế khách đang giữ, trả về địa chỉ trang thanh toán của MoMo. */
    public String startPayment(Long userId, Long showtimeId) {
        if (!isEnabled()) {
            throw new BusinessException("Rạp chưa bật thanh toán qua MoMo. Bạn chọn trả tại quầy nhé.");
        }
        List<Ticket> payable = paymentService.findPayableTickets(userId, showtimeId);
        if (payable.isEmpty()) {
            throw new InvalidBookingException(
                    "Không còn ghế nào đang giữ cho suất chiếu này. Bạn hãy chọn ghế lại.");
        }
        long amount = toVnd(paymentService.totalDue(userId, showtimeId));
        String orderId = newOrderId(showtimeId, userId);
        String orderInfo = "Thanh toan " + payable.size() + " ve UTE Cinema";
        return apiClient.createPayment(orderId, amount, orderInfo);
    }

    /**
     * Tạo giao dịch trả bằng mã QR cho các ghế khách đang giữ. Khách ở lại trang của rạp,
     * mở app MoMo quét mã; trang tự hỏi {@link #checkQrPayment} xem đã trả chưa.
     */
    public MomoQrPayment startQrPayment(Long userId, Long showtimeId) {
        if (!isEnabled()) {
            throw new BusinessException("Rạp chưa bật thanh toán qua MoMo. Bạn chọn trả tại quầy nhé.");
        }
        List<Ticket> payable = paymentService.findPayableTickets(userId, showtimeId);
        if (payable.isEmpty()) {
            throw new InvalidBookingException(
                    "Không còn ghế nào đang giữ cho suất chiếu này. Bạn hãy chọn ghế lại.");
        }
        long amount = toVnd(paymentService.totalDue(userId, showtimeId));
        String orderId = newOrderId(showtimeId, userId);
        MomoCheckout checkout = apiClient.createQrPayment(orderId, amount,
                "Thanh toan " + payable.size() + " ve UTE Cinema");
        return new MomoQrPayment(orderId, showtimeId, userId, amount,
                checkout.qrCodeUrl(), checkout.payUrl(), holdExpiresAt(payable));
    }

    /**
     * Hỏi MoMo xem giao dịch QR đã được trả chưa; đã trả thì xuất vé luôn.
     *
     * Chỉ chủ giao dịch mới hỏi được: mã đơn kẹp sẵn mã khách, lệch là từ chối. Gọi lặp lại
     * sau khi đã xuất vé vẫn an toàn - lần sau trả về ALREADY_PAID kèm đúng các vé đó.
     */
    public MomoPaymentResult checkQrPayment(String orderId, Long userId) {
        Matcher order = ORDER_ID_PATTERN.matcher(orderId == null ? "" : orderId);
        if (!isEnabled() || userId == null || !order.matches() || !userId.equals(Long.valueOf(order.group(2)))) {
            throw new BusinessException("Không tìm thấy giao dịch MoMo này trong tài khoản của bạn.");
        }
        Long showtimeId = Long.valueOf(order.group(1));

        MomoQueryResult status = apiClient.queryPayment(orderId);
        if (status.resultCode() == 0) {
            return settle(showtimeId, userId, status.transId(), status.amount());
        }
        if (PENDING_RESULT_CODES.contains(status.resultCode())) {
            return new MomoPaymentResult(Outcome.PENDING, showtimeId, userId, List.of(),
                    "Đang chờ bạn quét mã và xác nhận trên app MoMo.");
        }
        return new MomoPaymentResult(Outcome.FAILED, showtimeId, userId, List.of(),
                "MoMo báo giao dịch chưa thành công (" + status.message()
                        + "). Ghế vẫn được giữ trong thời gian còn lại, bạn có thể tạo mã mới hoặc chọn cách trả khác.");
    }

    /**
     * Xử lý kết quả MoMo gửi về, qua trình duyệt của khách hoặc qua IPN.
     *
     * Gọi nhiều lần với cùng một kết quả vẫn an toàn: khách bấm F5 ở trang kết quả, hoặc
     * trình duyệt và IPN về cùng lúc, thì lần sau chỉ báo "đã thanh toán rồi".
     */
    public MomoPaymentResult handleResult(Map<String, String> params) {
        verifySignature(params);
        Matcher order = ORDER_ID_PATTERN.matcher(value(params, "orderId"));
        if (!order.matches()) {
            throw new BusinessException("Mã đơn MoMo không phải của UTE Cinema.");
        }
        Long showtimeId = Long.valueOf(order.group(1));
        Long userId = Long.valueOf(order.group(2));
        String transId = value(params, "transId");

        if (!"0".equals(value(params, "resultCode"))) {
            return new MomoPaymentResult(Outcome.FAILED, showtimeId, userId, List.of(),
                    "Chưa thanh toán được qua MoMo (" + value(params, "message")
                            + "). Ghế vẫn được giữ trong thời gian còn lại, bạn có thể thử lại.");
        }

        return settle(showtimeId, userId, transId, Long.parseLong(value(params, "amount")));
    }

    /**
     * MoMo đã xác nhận khách trả {@code amount} đồng (mã giao dịch {@code transId}): xuất vé,
     * hoặc hoàn tiền nếu không xuất được. Dùng chung cho trang kết quả, IPN và thanh toán QR.
     */
    private MomoPaymentResult settle(Long showtimeId, Long userId, String transId, long amount) {
        List<Ticket> alreadyPaid = ticketRepository.findByPaymentRef(transId);
        if (!alreadyPaid.isEmpty()) {
            return paidResult(Outcome.ALREADY_PAID, showtimeId, userId, alreadyPaid);
        }

        List<Ticket> payable = paymentService.findPayableTickets(userId, showtimeId);
        if (payable.isEmpty()) {
            // Hết ghế đang giữ có thể vì một lần gọi khác (IPN, tab khác, lượt hỏi QR trước)
            // vừa xuất vé cho đúng giao dịch này. Kiểm lại trước khi hoàn tiền.
            List<Ticket> paidMeanwhile = ticketRepository.findByPaymentRef(transId);
            if (!paidMeanwhile.isEmpty()) {
                return paidResult(Outcome.ALREADY_PAID, showtimeId, userId, paidMeanwhile);
            }
            return refundBecauseTicketsCannotBeIssued(showtimeId, userId, transId, amount,
                    "Ghế đã hết thời gian giữ trước khi MoMo xác nhận nên không xuất được vé.");
        }
        if (toVnd(paymentService.totalDue(userId, showtimeId)) != amount) {
            return refundBecauseTicketsCannotBeIssued(showtimeId, userId, transId, amount,
                    "Số tiền đã trả không khớp với số ghế đang giữ nên không xuất được vé.");
        }

        try {
            List<Ticket> paid = paymentService.confirmPayment(userId, showtimeId, PaymentMethod.MOMO, transId);
            userRepository.findById(userId).ifPresent(customer ->
                    ticketMailService.sendTicketConfirmation(customer, paid, paymentService.sumPrice(paid)));
            return paidResult(Outcome.PAID, showtimeId, userId, paid);
        } catch (BusinessException exception) {
            // Có thể lần gọi kia (IPN hoặc tab khác) vừa xử lý xong cùng giao dịch này.
            List<Ticket> paidMeanwhile = ticketRepository.findByPaymentRef(transId);
            if (!paidMeanwhile.isEmpty()) {
                return paidResult(Outcome.ALREADY_PAID, showtimeId, userId, paidMeanwhile);
            }
            return refundBecauseTicketsCannotBeIssued(showtimeId, userId, transId, amount, exception.getMessage());
        }
    }

    /**
     * Kiểm chữ ký theo đúng thứ tự trường MoMo quy định. Sai chữ ký là ném lỗi ngay,
     * không đụng gì tới vé.
     */
    void verifySignature(Map<String, String> params) {
        if (!isEnabled()) {
            throw new BusinessException("Rạp chưa bật thanh toán qua MoMo.");
        }
        if (!properties.getPartnerCode().equals(value(params, "partnerCode"))) {
            throw new BusinessException("Kết quả thanh toán không phải gửi cho UTE Cinema.");
        }
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + value(params, "amount")
                + "&extraData=" + value(params, "extraData")
                + "&message=" + value(params, "message")
                + "&orderId=" + value(params, "orderId")
                + "&orderInfo=" + value(params, "orderInfo")
                + "&orderType=" + value(params, "orderType")
                + "&partnerCode=" + value(params, "partnerCode")
                + "&payType=" + value(params, "payType")
                + "&requestId=" + value(params, "requestId")
                + "&responseTime=" + value(params, "responseTime")
                + "&resultCode=" + value(params, "resultCode")
                + "&transId=" + value(params, "transId");
        byte[] expected = properties.sign(rawSignature).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = value(params, "signature").toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            log.warn("Chu ky MoMo khong khop cho don {}", value(params, "orderId"));
            throw new BusinessException("Kết quả thanh toán không hợp lệ (sai chữ ký). Vé chưa được xác nhận.");
        }
    }

    private MomoPaymentResult refundBecauseTicketsCannotBeIssued(Long showtimeId, Long userId, String transId,
                                                                 long amount, String reason) {
        try {
            String refundRef = apiClient.refund("UTE-HOAN-" + transId + "-" + System.currentTimeMillis(),
                    transId, amount, "Hoan tien do khong xuat duoc ve UTE Cinema");
            log.warn("Da tu hoan {} d cho giao dich MoMo {} (ma hoan {}): {}", amount, transId, refundRef, reason);
            return new MomoPaymentResult(Outcome.REFUNDED, showtimeId, userId, List.of(),
                    reason + " Hệ thống đã tự hoàn lại " + formatMoney(amount) + " về MoMo cho bạn.");
        } catch (BusinessException refundFailure) {
            log.error("KHONG tu hoan duoc giao dich MoMo {} ({} d): {}", transId, amount, refundFailure.getMessage());
            return new MomoPaymentResult(Outcome.REFUNDED, showtimeId, userId, List.of(),
                    reason + " Hệ thống chưa tự hoàn tiền được, bạn liên hệ quầy vé và đọc mã giao dịch MoMo "
                            + transId + " để được hoàn " + formatMoney(amount) + ".");
        }
    }

    private MomoPaymentResult paidResult(Outcome outcome, Long showtimeId, Long userId, List<Ticket> tickets) {
        return new MomoPaymentResult(outcome, showtimeId, userId,
                tickets.stream().map(Ticket::getId).toList(), "Thanh toán qua MoMo thành công.");
    }

    private static String newOrderId(Long showtimeId, Long userId) {
        return "UTE-" + showtimeId + "-" + userId + "-" + System.currentTimeMillis();
    }

    /** Ghế giữ sớm nhất hết hạn lúc nào - đó là hạn chót để trả tiền cho cả lượt. */
    private static LocalDateTime holdExpiresAt(List<Ticket> tickets) {
        return tickets.stream()
                .map(Ticket::getHeldAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(heldAt -> heldAt.plusMinutes(Constants.SEAT_HOLD_MINUTES))
                .orElse(LocalDateTime.now());
    }

    private static long toVnd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String formatMoney(long amount) {
        return String.format(Locale.US, "%,d đ", amount).replace(',', '.');
    }

    private static String value(Map<String, String> params, String key) {
        String value = params.get(key);
        return value == null ? "" : value;
    }
}
