package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Voucher;
import edu.hcmute.cnpm.cinema.repository.VoucherRepository;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Điều kiện ưu đãi và phân bổ giảm giá")
class PromotionServiceTest {
    private final LocalDate today = LocalDate.of(2026, 10, 2);
    private final BookingClock clock = new BookingClock(Clock.fixed(
            today.atStartOfDay(BookingClock.ZONE).toInstant(), BookingClock.ZONE));
    private Voucher offer(String code, int percent, String amount, LocalDate start, LocalDate end, boolean active) {
        Voucher voucher = new Voucher();
        voucher.setCode(code); voucher.setTitle("Ưu đãi"); voucher.setPercent(percent);
        voucher.setAmount(new BigDecimal(amount)); voucher.setMinimum(new BigDecimal("100000"));
        voucher.setMaximum(new BigDecimal("30000")); voucher.setStartsOn(start);
        voucher.setEndsOn(end); voucher.setActive(active);
        return voucher;
    }
    private PromotionService service(Voucher... offers) {
        VoucherRepository repository = mock(VoucherRepository.class);
        when(repository.findAllByOrderByCodeAsc()).thenReturn(List.of(offers));
        when(repository.findByCode(anyString())).thenAnswer(invocation -> List.of(offers).stream()
                .filter(offer -> offer.getCode().equals(invocation.getArgument(0))).findFirst());
        return new PromotionService(repository, clock);
    }

    @Test @DisplayName("Chuẩn hóa mã và giới hạn mức giảm phần trăm")
    void shouldNormalizeAndCapDiscount_whenPercentageOfferApplied() {
        var promotions = service(offer("UTE10", 10, "0", null, null, true));
        assertThat(promotions.discount(" ute10 ", new BigDecimal("150000"))).isEqualByComparingTo("15000");
        assertThat(promotions.discount("UTE10", new BigDecimal("500000"))).isEqualByComparingTo("30000");
        assertThatThrownBy(() -> promotions.discount("OTHER", new BigDecimal("150000"))).hasMessageContaining("không tồn tại");
        assertThatThrownBy(() -> promotions.discount("UTE10", new BigDecimal("99999")))
                .hasMessageContaining("tối thiểu 100.000 đ").hasMessageNotContaining("100000.00");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UTE 10", "<script>", "12345678901234567890123456789012345678901"})
    @DisplayName("Mã sai định dạng hoặc quá dài bị chặn trước truy vấn database")
    void shouldRejectMalformedCodesBeforeLookup(String code) {
        var repository = mock(VoucherRepository.class);
        var promotions = new PromotionService(repository, clock);
        assertThatThrownBy(() -> promotions.discount(code, new BigDecimal("150000")))
                .isInstanceOf(edu.hcmute.cnpm.cinema.exception.BusinessException.class).hasMessageContaining("40 ký tự");
        verifyNoInteractions(repository);
    }

    @Test @DisplayName("Chỉ hiển thị mã đang có hiệu lực; ngày cuối vẫn được dùng")
    void shouldCheckValidityDates_whenListingOffers() {
        var promotions = service(offer("CURRENT", 0, "20000", today, today, true),
                offer("OLD", 10, "0", null, today.minusDays(1), true),
                offer("FUTURE", 10, "0", today.plusDays(1), null, true),
                offer("OFF", 10, "0", null, null, false));
        assertThat(promotions.availableOffers()).extracting(Voucher::getCode).containsExactly("CURRENT");
        assertThat(promotions.discount("CURRENT", new BigDecimal("150000"))).isEqualByComparingTo("20000");
        for (String code : List.of("OLD", "FUTURE", "OFF"))
            assertThatThrownBy(() -> promotions.discount(code, new BigDecimal("150000"))).hasMessageContaining("hiệu lực");
    }

    @Test @DisplayName("Thông báo UTE20K định dạng tiền từ cột decimal trong database")
    void shouldFormatMinimumFromDatabaseScale() {
        Voucher voucher = offer("UTE20K", 0, "20000", null, null, true);
        voucher.setMinimum(new BigDecimal("200000.00"));
        assertThatThrownBy(() -> service(voucher).discount("UTE20K", new BigDecimal("199999.00")))
                .hasMessage("Mã UTE20K yêu cầu tiền vé tối thiểu 200.000 đ (không tính bắp nước).");
    }

    @Test @DisplayName("Đổi tổng vé làm mã không đủ điều kiện thì bỏ mã; bắp nước không được giảm")
    void shouldRecalculate_whenOrderChanges() {
        var promotions = service(offer("UTE10", 10, "0", null, null, true));
        BookingOrder order = new BookingOrder();
        order.setTicketSubtotal(new BigDecimal("150000"));
        order.setConcessionSubtotal(new BigDecimal("79000"));
        order.setVoucherCode("UTE10");
        promotions.recalculate(order);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("214000");
        order.setTicketSubtotal(new BigDecimal("75000"));
        promotions.recalculate(order);
        assertThat(order.getVoucherCode()).isNull();
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("154000");
    }

    @Test @DisplayName("Đơn đã trả và đơn đã hủy không bị thay đổi khi voucher hết hiệu lực")
    void shouldKeepClosedOrderAmounts_whenRecalculationRequested() {
        var promotions = service(offer("UTE10", 10, "0", null, null, false));
        for (var status : List.of(edu.hcmute.cnpm.cinema.entity.BookingOrderStatus.PAID,
                edu.hcmute.cnpm.cinema.entity.BookingOrderStatus.CANCELLED)) {
            BookingOrder order = new BookingOrder();
            order.setStatus(status);
            order.setVoucherCode("UTE10");
            order.setAppliedVoucherCode("UTE10");
            order.setAppliedVoucherName("Ưu đãi đã chốt");
            order.setDiscountAmount(new BigDecimal("20000"));
            order.setTotalAmount(new BigDecimal("180000"));
            promotions.recalculate(order);
            assertThat(order.getVoucherCode()).isEqualTo("UTE10");
            assertThat(order.getAppliedVoucherName()).isEqualTo("Ưu đãi đã chốt");
            assertThat(order.getDiscountAmount()).isEqualByComparingTo("20000");
            assertThat(order.getTotalAmount()).isEqualByComparingTo("180000");
        }
    }

    @Test @DisplayName("Phân bổ giảm giá giữ nguyên tổng tiền kể cả khi phải làm tròn")
    void shouldAllocateExactly_whenTicketsHaveDifferentPrices() {
        var promotions = service();
        BookingOrder order = new BookingOrder();
        order.setTicketSubtotal(new BigDecimal("300003"));
        order.setDiscountAmount(new BigDecimal("20000"));
        Ticket first = new Ticket(); first.setId(1L); first.setPrice(new BigDecimal("100001"));
        Ticket second = new Ticket(); second.setId(2L); second.setPrice(new BigDecimal("200002"));
        promotions.allocateDiscount(order, List.of(second, first));
        assertThat(first.getPrice().add(second.getPrice())).isEqualByComparingTo("280003");
        assertThat(first.getOriginalPrice()).isEqualByComparingTo("100001");
        assertThat(second.getOriginalPrice()).isEqualByComparingTo("200002");
        assertThat(first.getPrice()).isEqualByComparingTo("93335");
    }
}
