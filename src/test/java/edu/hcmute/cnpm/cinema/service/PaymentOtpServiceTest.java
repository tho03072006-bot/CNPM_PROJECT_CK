package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("OTP thanh toán gắn với đúng tài khoản, phương thức và đơn vé")
class PaymentOtpServiceTest {
    @Test @DisplayName("Đổi tài khoản, suất chiếu, phương thức hoặc giá tiền không dùng được mã cũ")
    void shouldRejectChangesToPayment() {
        AuthService auth = mock(AuthService.class);
        PaymentService payments = mock(PaymentService.class);
        MailDelivery mail = mock(MailDelivery.class);
        String[] code = new String[1];
        when(mail.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            var matcher = Pattern.compile("[0-9]{6}").matcher((String) invocation.getArgument(2));
            matcher.find(); code[0] = matcher.group(); return true;
        });
        User user = new User(); user.setEmail("payment@gmail.com");
        when(auth.findById(1L)).thenReturn(user);
        Ticket ticket = new Ticket(); ticket.setId(1L); ticket.setPrice(new BigDecimal("100000")); ticket.setHeldAt(LocalDateTime.now());
        when(payments.findPayableTickets(1L, 2L)).thenReturn(List.of(ticket));
        var service = new PaymentOtpService(auth, payments, new OtpService(mail));
        String id = service.begin(1L, 2L, "momo-qr");
        assertThatThrownBy(() -> service.verify(id, 3L, 2L, "momo-qr", code[0], () -> true)).hasMessageContaining("thay đổi");
        assertThatThrownBy(() -> service.verify(id, 1L, 3L, "momo-qr", code[0], () -> true)).hasMessageContaining("thay đổi");
        assertThatThrownBy(() -> service.verify(id, 1L, 2L, "momo", code[0], () -> true)).hasMessageContaining("thay đổi");
        ticket.setPrice(new BigDecimal("200000"));
        assertThatThrownBy(() -> service.verify(id, 1L, 2L, "momo-qr", code[0], () -> true)).hasMessageContaining("thay đổi");
        ticket.setPrice(new BigDecimal("100000"));
        assertThat(service.verify(id, 1L, 2L, "momo-qr", code[0], () -> true)).isTrue();
    }
}
