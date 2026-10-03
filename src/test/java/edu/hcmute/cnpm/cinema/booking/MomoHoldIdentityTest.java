package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.config.MomoProperties;
import edu.hcmute.cnpm.cinema.dto.payment.*;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.*;
import edu.hcmute.cnpm.cinema.service.*;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MomoHoldIdentityTest {
    private final MomoApiClient api = mock(MomoApiClient.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final MomoPaymentService momo = new MomoPaymentService(
            new MomoProperties("http://localhost:1", "TEST", "access", "secret", "payWithMethod", "http://localhost"),
            api, payments, tickets, mock(UserRepository.class), mock(TicketMailService.class));
    private Ticket current;
    @BeforeEach
    void prepare() {
        BookingTestFixture fixture = new BookingTestFixture();
        current = fixture.testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        current.setId(20L);
        when(payments.findPayableTickets(1L,1L)).thenReturn(List.of(current));
        when(api.refund(anyString(),eq("123"),eq(75000L),anyString())).thenReturn("refund");
    }
    @Test @DisplayName("QR cũ cùng giá không thanh toán ghế mới")
    void shouldRefundOldQr_whenHoldWasReplaced() {
        when(api.queryPayment("UTE-1-1-10-999")).thenReturn(new MomoQueryResult(0,"ok","123",75000));
        assertThat(momo.checkQrPayment("UTE-1-1-10-999",1L).getOutcome()).isEqualTo(MomoPaymentResult.Outcome.REFUNDED);
        verify(payments,never()).confirmPayment(anyLong(),anyLong(),any(),anyString(),anyList(),anyLong(),anyLong());
        verify(api).refund(eq("UTE-HOAN-123"),eq("123"),eq(75000L),anyString());
    }
    @Test @DisplayName("QR định dạng cũ thiếu mã lượt giữ được hoàn, không gắn với ghế mới")
    void shouldRefundLegacyQr_whenOrderHasNoHoldIdentity() {
        when(api.queryPayment("UTE-1-1-999")).thenReturn(new MomoQueryResult(0,"ok","123",75000));
        assertThat(momo.checkQrPayment("UTE-1-1-999",1L).getOutcome()).isEqualTo(MomoPaymentResult.Outcome.REFUNDED);
        verify(payments,never()).confirmPayment(anyLong(),anyLong(),any(),anyString(),anyList(),anyLong(),anyLong());
    }
    @Test @DisplayName("Đổi ghế trong lúc xử lý callback vẫn bị chặn trong giao dịch thanh toán")
    void shouldRefund_whenAtomicPaymentDetectsReplacementRace() {
        when(api.queryPayment("UTE-1-1-20-999")).thenReturn(new MomoQueryResult(0,"ok","123",75000));
        when(payments.confirmPayment(1L,1L,PaymentMethod.MOMO,"123",List.of(20L),20L,75000L))
                .thenThrow(new InvalidBookingException("Lượt giữ đã thay đổi."));
        assertThat(momo.checkQrPayment("UTE-1-1-20-999",1L).getOutcome()).isEqualTo(MomoPaymentResult.Outcome.REFUNDED);
        verify(api).refund(eq("UTE-HOAN-123"),eq("123"),eq(75000L),anyString());
    }
    @Test @DisplayName("Callback hoàn tiền lặp lại dùng cùng requestId")
    void shouldReuseRefundIdentity_whenCallbackIsRetried() {
        when(api.queryPayment("UTE-1-1-10-999")).thenReturn(new MomoQueryResult(0,"ok","123",75000));
        momo.checkQrPayment("UTE-1-1-10-999",1L);momo.checkQrPayment("UTE-1-1-10-999",1L);
        verify(api,times(2)).refund(eq("UTE-HOAN-123"),eq("123"),eq(75000L),anyString());
    }
    @Test @DisplayName("Mã vượt Long nhận lỗi nghiệp vụ trước khi hỏi MoMo")
    void shouldRejectOversizedOrderId_whenIdentifierOverflows() {
        assertThatThrownBy(() -> momo.checkQrPayment("UTE-1-99999999999999999999-20-999",1L))
                .isInstanceOf(edu.hcmute.cnpm.cinema.exception.BusinessException.class);
        verifyNoInteractions(api);
    }
}
