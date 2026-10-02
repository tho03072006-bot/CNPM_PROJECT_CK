package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OTP: hết hạn, chống dò mã và dùng lại")
class OtpServiceTest {
    private MailDelivery mail;
    private OtpService otp;
    private final MutableClock clock = new MutableClock();
    private String code;
    @BeforeEach void setup() {
        mail = mock(MailDelivery.class);
        when(mail.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            var matcher = Pattern.compile("[0-9]{6}").matcher((String) invocation.getArgument(2));
            assertThat(matcher.find()).isTrue();
            code = matcher.group();
            return true;
        });
        otp = new OtpService(mail, clock);
    }
    private String begin() { return otp.begin(OtpService.Purpose.REGISTER, "an@gmail.com", "payload", true); }

    @Test @DisplayName("Mã đúng chỉ chạy nghiệp vụ một lần")
    void shouldRejectReuse_whenAlreadyVerified() {
        String id = begin();
        assertThat(otp.verify(id, OtpService.Purpose.REGISTER, code, () -> "done")).isEqualTo("done");
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, code, () -> "again"))
                .isInstanceOf(BusinessException.class);
    }
    @Test @DisplayName("Sai mục đích không xác thực được")
    void shouldRejectCode_whenPurposeDiffers() {
        String id = begin();
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.PAYMENT, code, () -> true)).isInstanceOf(BusinessException.class);
    }
    @Test @DisplayName("Mã hết hạn đúng tại mốc 5 phút")
    void shouldExpire_whenFiveMinutesPassed() {
        String id = begin(); clock.advance(300);
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, code, () -> true)).hasMessageContaining("hết hạn");
    }
    @Test @DisplayName("Nhập sai 5 lần khoá mã, kể cả sau đó nhập đúng")
    void shouldLock_whenFiveAttemptsFailed() {
        String id = begin();
        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, "invalid", () -> true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, code, () -> true)).hasMessageContaining("5 lần");
    }
    @Test @DisplayName("Gửi lại sau 60 giây, mã cũ mất hiệu lực")
    void shouldRotateCode_whenResentAfterCooldown() {
        String id = begin(); String old = code;
        assertThatThrownBy(() -> otp.resend(id, OtpService.Purpose.REGISTER)).hasMessageContaining("60 giây");
        clock.advance(60); otp.resend(id, OtpService.Purpose.REGISTER);
        assertThat(code).isNotEqualTo(old);
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, old, () -> true)).isInstanceOf(BusinessException.class);
        assertThat(otp.verify(id, OtpService.Purpose.REGISTER, code, () -> true)).isTrue();
    }
    @Test @DisplayName("Tối đa 5 lần gửi mỗi email trong 15 phút, kể cả bắt đầu phiên khác")
    void shouldLimitSendRate_acrossChallenges() {
        for (int i = 0; i < 5; i++) { begin(); clock.advance(60); }
        assertThatThrownBy(this::begin).hasMessageContaining("15 phút");
        clock.advance(600); assertThat(begin()).isNotBlank();
    }
    @Test @DisplayName("SMTP lỗi không tạo phiên đăng ký thành công")
    void shouldFailClosed_whenMailFails() {
        doReturn(false).when(mail).send(anyString(), anyString(), anyString());
        assertThatThrownBy(this::begin).hasMessageContaining("Chưa gửi được");
    }
    @Test @DisplayName("Khôi phục email không tồn tại vẫn có trang xác thực giống nhau")
    void shouldHideExistence_whenRecoveryIsRequested() {
        String id = otp.begin(OtpService.Purpose.RESET, "absent@gmail.com", "dummy", false);
        assertThat(otp.describe(id, OtpService.Purpose.RESET).email()).isEqualTo("a***@gmail.com");
        verifyNoInteractions(mail);
    }
    @Test @DisplayName("Nghiệp vụ thất bại giữ mã để thử lại")
    void shouldKeepCode_whenActionFails() {
        String id = begin();
        assertThatThrownBy(() -> otp.verify(id, OtpService.Purpose.REGISTER, code, () -> { throw new BusinessException("failure"); }))
                .isInstanceOf(BusinessException.class);
        assertThat(otp.verify(id, OtpService.Purpose.REGISTER, code, () -> true)).isTrue();
    }
    @Test @DisplayName("Hai request đồng thời chỉ có một request thực thi nghiệp vụ")
    void shouldExecuteOnce_whenConcurrent() throws Exception {
        String id = begin(); AtomicInteger count = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var task = (java.util.concurrent.Callable<Boolean>) () -> {
                try { return otp.verify(id, OtpService.Purpose.REGISTER, code, () -> { count.incrementAndGet(); return true; }); }
                catch (BusinessException e) { return false; }
            };
            var a = pool.submit(task); var b = pool.submit(task);
            assertThat(a.get() ^ b.get()).isTrue();
            assertThat(count.get()).isEqualTo(1);
        }
    }
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-30T00:00:00Z");
        void advance(long seconds) { now = now.plusSeconds(seconds); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
