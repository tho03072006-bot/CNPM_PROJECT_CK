package edu.hcmute.cnpm.cinema.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Luật chung cho mọi thư gửi đi. Unit test, không cần database hay máy chủ mail. */
@DisplayName("Gửi email: công tắc, tên miền thử nghiệm, người gửi")
class MailDeliveryTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);

    @Test
    @DisplayName("Tắt công tắc thì không đụng tới máy chủ mail")
    void shouldNotSend_whenMailDisabled() {
        when(provider.getIfAvailable()).thenReturn(sender);
        MailDelivery delivery = new MailDelivery(provider, false, "rap@gmail.com", "");

        assertThat(delivery.send("khach@gmail.com", "Tiêu đề", "Nội dung")).isFalse();
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Không gửi tới tên miền dành cho thử nghiệm như .local hay example.com")
    void shouldSkipReservedTestDomains() {
        assertThat(MailDelivery.isReservedAddress("khachhang@utecinema.local")).isTrue();
        assertThat(MailDelivery.isReservedAddress("a@example.com")).isTrue();
        assertThat(MailDelivery.isReservedAddress("b@thu.test")).isTrue();
        assertThat(MailDelivery.isReservedAddress("khach@gmail.com")).isFalse();
    }

    @Test
    @DisplayName("Đủ cấu hình và địa chỉ thật thì gửi, người gửi là tài khoản Gmail đang dùng")
    void shouldSendFromConfiguredAccount_whenReady() {
        when(provider.getIfAvailable()).thenReturn(sender);
        MailDelivery delivery = new MailDelivery(provider, true, "rap@gmail.com", "");

        assertThat(delivery.send("khach@gmail.com", "Tiêu đề", "Nội dung")).isTrue();
        verify(sender).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>argThat(message ->
                "UTE Cinema <rap@gmail.com>".equals(message.getFrom())
                        && "khach@gmail.com".equals(message.getTo()[0])));
    }

    @Test
    @DisplayName("Máy chủ mail báo lỗi thì chỉ ghi log, không ném lỗi ra làm hỏng giao dịch")
    void shouldSwallowError_whenSmtpFails() {
        when(provider.getIfAvailable()).thenReturn(sender);
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP hỏng"))
                .when(sender).send(any(SimpleMailMessage.class));
        MailDelivery delivery = new MailDelivery(provider, true, "rap@gmail.com", "");

        assertThat(delivery.send("khach@gmail.com", "Tiêu đề", "Nội dung")).isFalse();
    }
}
