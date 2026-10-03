package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.BookingOrderRepository;
import edu.hcmute.cnpm.cinema.repository.ConcessionProductRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Nghiệp vụ đơn hàng, bắp nước và hóa đơn điện tử. */
@Service
public class BookingOrderService {

    private static final int MAX_QUANTITY_PER_PRODUCT = 10;
    private static final int MAX_ITEMS_PER_ORDER = 20;
    private static final DateTimeFormatter RECEIPT_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BookingLockService locks;
    private final BookingClock clock;
    private final BookingOrderRepository bookingOrderRepository;
    private final ConcessionProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ConcessionInventoryService inventoryService;
    private final BookingSnapshotService snapshots;
    private final PromotionService promotionService;

    public BookingOrderService(BookingOrderRepository bookingOrderRepository,
                               ConcessionProductRepository productRepository,
                               TicketRepository ticketRepository,
                               UserRepository userRepository,
                               ShowtimeRepository showtimeRepository,
                               ConcessionInventoryService inventoryService,
                               BookingLockService locks,
                               BookingClock clock, BookingSnapshotService snapshots, PromotionService promotionService) {
        this.promotionService = promotionService;
        this.locks = locks;
        this.clock = clock;
        this.bookingOrderRepository = bookingOrderRepository;
        this.productRepository = productRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.showtimeRepository = showtimeRepository;
        this.inventoryService = inventoryService;
        this.snapshots = snapshots;
    }

    @Transactional(readOnly = true)
    public List<ConcessionProduct> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    /** Tạo hoặc cập nhật đơn nháp, kể cả khi khách chọn không mua bắp nước. */
    @Transactional
    public BookingOrder saveConcessions(Long userId, Long showtimeId, Map<Long, Integer> quantities) {
        locks.lock(showtimeId);
        List<Ticket> heldTickets = findValidHeldTickets(userId, showtimeId);
        BookingOrder order = prepareDraft(userId, showtimeId, heldTickets);

        Map<Long, Integer> selected = normalizeQuantities(quantities);
        List<ConcessionProduct> products = selected.isEmpty()
                ? List.of() : productRepository.findAllById(selected.keySet());
        if (products.size() != selected.size() || products.stream().anyMatch(product -> !product.isActive())) {
            throw new BusinessException("Có món không còn được bán. Bạn tải lại danh sách bắp nước và chọn lại nhé.");
        }
        Map<ConcessionProduct, Integer> selection = new LinkedHashMap<>();
        for (ConcessionProduct product : products) {
            selection.put(product, selected.get(product.getId()));
        }
        inventoryService.requireInStock(selection);

        List<BookingOrderItem> items = new ArrayList<>();
        BigDecimal concessionSubtotal = BigDecimal.ZERO;
        for (ConcessionProduct product : products) {
            int quantity = selected.get(product.getId());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            BookingOrderItem item = new BookingOrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setLineTotal(lineTotal);
            items.add(item);
            concessionSubtotal = concessionSubtotal.add(lineTotal);
        }
        items.sort(Comparator.comparing(item -> item.getProduct().getDisplayOrder()));
        boolean selectionChanged = order.getItems().size() != items.size() || items.stream().anyMatch(item ->
                order.getItems().stream().noneMatch(old -> old.getProduct().getId().equals(item.getProduct().getId())
                        && old.getQuantity() == item.getQuantity() && old.getUnitPrice().compareTo(item.getUnitPrice()) == 0));
        order.replaceItems(items);
        if ((selectionChanged || order.getConcessionSubtotal().compareTo(concessionSubtotal) != 0)
                && order.getVoucherCode() == null) clearOffer(order);
        order.setConcessionSubtotal(concessionSubtotal);
        recalculateTotal(order);
        return bookingOrderRepository.save(order);
    }

    /** Bảo đảm đường dẫn thanh toán trực tiếp vẫn có một đơn không kèm bắp nước. */
    @Transactional
    public BookingOrder prepareForPayment(Long userId, Long showtimeId) {
        locks.lock(showtimeId);
        List<Ticket> heldTickets = findValidHeldTickets(userId, showtimeId);
        return prepareDraft(userId, showtimeId, heldTickets);
    }

    /** Điểm nối cho module ưu đãi: chỉ gọi sau khi dịch vụ voucher xác thực mã và điều kiện. */
    @Transactional
    public BookingOrder applyValidatedOffer(Long userId, Long showtimeId, String code, String name, BigDecimal amount) {
        BookingOrder order = prepareForPayment(userId, showtimeId);
        BigDecimal subtotal = order.getTicketSubtotal().add(order.getConcessionSubtotal());
        if (code == null || code.isBlank() || code.strip().length() > 50 || name == null || name.isBlank()
                || name.strip().length() > 200 || amount == null || amount.signum() <= 0
                || amount.stripTrailingZeros().scale() > 0 || amount.compareTo(subtotal) > 0) {
            throw new InvalidBookingException("Thông tin ưu đãi đã xác thực không hợp lệ.");
        }
        clearOffer(order);
        order.setAppliedVoucherCode(code.strip());
        order.setAppliedVoucherName(name.strip());
        order.setDiscountAmount(amount);
        recalculateTotal(order);
        return bookingOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> selectedQuantities(Long userId, Long showtimeId) {
        return bookingOrderRepository
                .findFirstByUserIdAndShowtimeIdAndStatusOrderByCreatedAtDesc(
                        userId, showtimeId, BookingOrderStatus.DRAFT)
                .map(order -> {
                    Map<Long, Integer> result = new HashMap<>();
                    for (BookingOrderItem item : order.getItems()) {
                        result.put(item.getProduct().getId(), item.getQuantity());
                    }
                    return result;
                })
                .orElseGet(HashMap::new);
    }

    /** Chốt đơn cùng giao dịch đã đổi vé sang PAID. Phải được gọi trong transaction thanh toán. */
    @Transactional
    public BookingOrder completeOrder(Long userId, Long showtimeId, List<Ticket> tickets,
                                      PaymentMethod paymentMethod, String paymentRef, LocalDateTime paidAt) {
        locks.lock(showtimeId);
        BookingOrder order = prepareDraft(userId, showtimeId, tickets);
        // Hết hàng thì ném lỗi ở đây, cả lần thanh toán bị hủy (MoMo tự hoàn tiền).
        inventoryService.deductForPaidOrder(order);
        if (order.getVoucherCode() != null) promotionService.allocateDiscount(order, tickets);
        order.setStatus(BookingOrderStatus.PAID);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentRef(paymentRef);
        order.setPaidAt(paidAt);
        for (Ticket ticket : tickets) {
            ticket.setBookingOrder(order);
        }
        bookingOrderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = true)
    public BookingOrder findReceiptForUser(String receiptCode, User viewer) {
        BookingOrder order = bookingOrderRepository.findByReceiptCode(receiptCode)
                .orElseThrow(() -> new ResourceNotFoundException("hóa đơn", receiptCode));
        if (order.getStatus() != BookingOrderStatus.PAID) {
            throw new ResourceNotFoundException("Hóa đơn này chưa được phát hành.");
        }
        boolean owner = viewer != null && order.getUser().getId().equals(viewer.getId());
        boolean employee = viewer != null && (viewer.getRole() == Role.STAFF || viewer.getRole() == Role.ADMIN);
        if (!owner && !employee) {
            throw new BusinessException("Bạn không có quyền xem hóa đơn này.");
        }
        // Khởi tạo dữ liệu khi còn transaction để trang in không phụ thuộc Open Session in View.
        order.getItems().size();
        order.getTickets().size();
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<String> findReceiptCodeByTicketId(Long ticketId, Long userId) {
        return bookingOrderRepository.findFirstByTicketsId(ticketId)
                .filter(order -> order.getStatus() == BookingOrderStatus.PAID)
                .filter(order -> order.getUser().getId().equals(userId))
                .map(BookingOrder::getReceiptCode);
    }

    private BookingOrder prepareDraft(Long userId, Long showtimeId, List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            throw new InvalidBookingException("Không còn ghế nào đang giữ. Bạn hãy chọn ghế lại.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("tài khoản", userId));
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("suất chiếu", showtimeId));

        BookingOrder order = bookingOrderRepository
                .findFirstByUserIdAndShowtimeIdAndStatusOrderByCreatedAtDesc(
                        userId, showtimeId, BookingOrderStatus.DRAFT)
                .orElseGet(() -> newDraft(user, showtime));

        if (order.getId() != null && !snapshots.belongsToSameHold(order, tickets)) {
            order.setStatus(BookingOrderStatus.CANCELLED);
            bookingOrderRepository.save(order);
            order = newDraft(user, showtime);
        }

        BigDecimal ticketSubtotal = tickets.stream()
                .map(Ticket::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<Long> previousIds = new HashSet<>(snapshots.read(order).stream()
                .map(edu.hcmute.cnpm.cinema.dto.booking.BookedTicketSnapshot::ticketId).toList());
        Set<Long> nextIds = new HashSet<>(tickets.stream().map(Ticket::getId).toList());
        if (!previousIds.equals(nextIds) || order.getTicketSubtotal().compareTo(ticketSubtotal) != 0) clearOffer(order);
        order.setTicketSubtotal(ticketSubtotal);
        recalculateTotal(order);
        snapshots.capture(order, tickets);
        order = bookingOrderRepository.save(order);
        for (Ticket ticket : tickets) {
            ticket.setBookingOrder(order);
        }
        ticketRepository.saveAll(tickets);
        return order;
    }

    @Transactional
    public BookingOrder applyVoucher(Long userId, Long showtimeId, String code, List<Long> expectedIds) {
        locks.lock(showtimeId);
        List<Ticket> tickets = findValidHeldTickets(userId, showtimeId);
        if (expectedIds == null) throw new InvalidBookingException("Thiếu mã lượt giữ ghế. Vui lòng tải lại trang.");
        HoldIdentity.requireMatch(expectedIds, tickets.stream().map(Ticket::getId).toList());
        BookingOrder order = prepareDraft(userId, showtimeId, tickets);
        String normalized = promotionService.normalize(code);
        if (!normalized.isEmpty()) promotionService.discount(normalized, order.getTicketSubtotal());
        clearOffer(order);
        order.setVoucherCode(normalized.isEmpty() ? null : normalized);
        recalculateTotal(order);
        return bookingOrderRepository.save(order);
    }

    private void clearOffer(BookingOrder order) {
        order.setVoucherCode(null);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setAppliedVoucherCode(null);
        order.setAppliedVoucherName(null);
    }

    private void recalculateTotal(BookingOrder order) {
        if (order.getVoucherCode() != null) {
            promotionService.recalculate(order);
            return;
        }
        BigDecimal subtotal = order.getTicketSubtotal().add(order.getConcessionSubtotal());
        if (order.getDiscountAmount().signum() < 0 || order.getDiscountAmount().compareTo(subtotal) > 0) {
            throw new InvalidBookingException("Số tiền giảm không phù hợp với đơn đặt vé.");
        }
        order.setTotalAmount(subtotal.subtract(order.getDiscountAmount()));
    }

    private BookingOrder newDraft(User user, Showtime showtime) {
        BookingOrder order = new BookingOrder();
        order.setReceiptCode(newReceiptCode());
        order.setUser(user);
        order.setShowtime(showtime);
        order.setCustomerName(user.getFullName());
        order.setCustomerEmail(user.getEmail());
        order.setMovieTitle(showtime.getMovie().getTitle());
        order.setRoomName(showtime.getRoom().getName());
        order.setShowtimeStart(showtime.getStartTime());
        return order;
    }

    private List<Ticket> findValidHeldTickets(Long userId, Long showtimeId) {
        List<Ticket> tickets = ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);
        if (tickets.isEmpty() || tickets.stream().anyMatch(clock::expired)
                || !tickets.getFirst().getShowtime().getStartTime().isAfter(clock.now())) {
            throw new InvalidBookingException("Không còn lượt giữ ghế hợp lệ. Bạn hãy chọn ghế lại.");
        }
        return tickets;
    }

    private Map<Long, Integer> normalizeQuantities(Map<Long, Integer> quantities) {
        Map<Long, Integer> selected = new HashMap<>();
        int totalQuantity = 0;
        if (quantities != null) {
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                int quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (quantity < 0 || quantity > MAX_QUANTITY_PER_PRODUCT) {
                    throw new BusinessException("Mỗi món được chọn tối đa " + MAX_QUANTITY_PER_PRODUCT + " phần.");
                }
                if (quantity > 0) {
                    selected.put(entry.getKey(), quantity);
                    totalQuantity += quantity;
                }
            }
        }
        if (totalQuantity > MAX_ITEMS_PER_ORDER) {
            throw new BusinessException("Mỗi đơn được chọn tối đa " + MAX_ITEMS_PER_ORDER + " phần bắp nước.");
        }
        return selected;
    }

    private String newReceiptCode() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "UTE-" + clock.now().format(RECEIPT_TIME) + "-" + random;
    }
}
