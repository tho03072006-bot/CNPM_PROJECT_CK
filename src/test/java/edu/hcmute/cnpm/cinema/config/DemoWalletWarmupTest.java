package edu.hcmute.cnpm.cinema.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.io.IOException;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoWalletWarmupTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldWakeOnlyTheHealthEndpointAfterNormalizingWalletUrl() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"status\":\"UP\"}");
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test/demo-wallet/"), new ObjectMapper(), client);
        warmup.warmUp();
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri().toString()).isEqualTo("https://g8.test/demo-wallet/health");
        assertThat(request.getValue().method()).isEqualTo("GET");
    }

    @Test
    void shouldKeepLocalCinemaRunningWhenWalletCannotBeReached() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(), any())).thenThrow(new IOException("Sensitive network details must not be logged"));
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test"), new ObjectMapper(), client);
        assertThatCode(warmup::warmUp).doesNotThrowAnyException();
    }

    @Test
    void shouldSkipWarmupWhenSimulatedWalletIsDisabled() {
        HttpClient client = mock(HttpClient.class);
        var warmup = new DemoWalletWarmup(new DemoWalletSettings(false, ""), new ObjectMapper(), client);
        warmup.start();
        verifyNoInteractions(client);
    }

    @Test
    void shouldFailBeforeCheckoutWhenPublicWalletUrlIsInvalid() {
        assertThatThrownBy(() -> new DemoWalletWarmup(new DemoWalletSettings(true, "https://g8.test/pay/123"),
                new ObjectMapper(), mock(HttpClient.class))).hasMessageContaining("Địa chỉ ví");
    }
}
