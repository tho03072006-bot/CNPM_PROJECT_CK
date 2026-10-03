package edu.hcmute.cnpm.cinema.config;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoWalletWarmupTest {
    @Test
    void shouldWakeOnlyTheHealthEndpointAfterNormalizingWalletUrl() throws Exception {
        DemoWalletCompatibility compatibility = mock(DemoWalletCompatibility.class);
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test/demo-wallet/"), compatibility);
        warmup.warmUp();
        verify(compatibility).warmUp();
    }

    @Test
    void shouldKeepLocalCinemaRunningWhenWalletCannotBeReached() throws Exception {
        DemoWalletCompatibility compatibility = mock(DemoWalletCompatibility.class);
        doThrow(new BusinessException("Ví chưa đồng bộ")).when(compatibility).warmUp();
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test"), compatibility);
        assertThatCode(warmup::warmUp).doesNotThrowAnyException();
    }

    @Test
    void shouldSkipWarmupWhenSimulatedWalletIsDisabled() {
        DemoWalletCompatibility compatibility = mock(DemoWalletCompatibility.class);
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(false, ""), compatibility);
        warmup.start();
        verifyNoInteractions(compatibility);
    }

    @Test
    void shouldFailBeforeCheckoutWhenPublicWalletUrlIsInvalid() {
        assertThatThrownBy(() -> new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test/pay/123"),
                mock(DemoWalletCompatibility.class))).hasMessageContaining("Địa chỉ ví");
    }
}
