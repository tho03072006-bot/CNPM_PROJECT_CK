package edu.hcmute.cnpm.cinema.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.io.IOException;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoWalletCompatibilityTest {
    private final HttpClient client = mock(HttpClient.class);
    private final AtomicLong time = new AtomicLong();
    private DemoWalletCompatibility checker(boolean phone) {
        return new DemoWalletCompatibility(new DemoWalletSettings(true, "https://g8.test/demo-wallet/"),
                new ObjectMapper(), phone, client, time::get);
    }
    @SuppressWarnings("unchecked")
    private void response(int code, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(code); when(response.body()).thenReturn(body);
        when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
    @Test void acceptsOnlyHealthyCompatibleCheckoutAndCallsOnlyHealth() throws Exception {
        response(200, "{\"status\":\"UP\",\"checkoutVersion\":\"2\"}");
        var checker = checker(false);
        checker.requireCompatible(); checker.requireCompatible();
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any());
        assertThat(request.getValue().uri().toString()).isEqualTo("https://g8.test/demo-wallet/health");
        assertThat(request.getValue().method()).isEqualTo("GET");
        assertThat(request.getValue().timeout()).contains(Duration.ofSeconds(6));
    }
    @ParameterizedTest @ValueSource(strings={"{\"status\":\"UP\"}", "{\"status\":\"UP\",\"checkoutVersion\":\"1\"}",
            "{\"status\":\"UP\",\"checkoutVersion\":\"3\"}"})
    void rejectsLegacyOrDifferentCheckoutEvenWhenWalletReportsUp(String body) throws Exception {
        response(200, body);
        assertThatThrownBy(checker(false)::requireCompatible).isInstanceOf(BusinessException.class).hasMessageContaining("chưa đồng bộ");
    }
    @ParameterizedTest @ValueSource(strings={"{\"status\":\"DOWN\",\"checkoutVersion\":\"2\"}", "null", "[]", "not-json"})
    void rejectsInvalidOrUnhealthyResponses(String body) throws Exception {
        response(200, body);
        assertThatThrownBy(checker(false)::requireCompatible).isInstanceOf(BusinessException.class).hasMessageContaining("Chưa kết nối");
    }
    @Test void doesNotTrustRedirectsAndDoesNotExposeNetworkDetails() throws Exception {
        response(302, "{\"status\":\"UP\",\"checkoutVersion\":\"2\"}");
        assertThatThrownBy(checker(false)::requireCompatible).hasMessageContaining("Chưa kết nối");
        when(client.send(any(), any())).thenThrow(new IOException("private network details"));
        assertThatThrownBy(checker(false)::requireCompatible).hasMessageNotContaining("private");
    }
    @Test void rechecksAfterUpdateAndAfterCachedSuccessExpires() throws Exception {
        response(200, "{\"status\":\"UP\"}"); var checker = checker(false);
        assertThatThrownBy(checker::requireCompatible).isInstanceOf(BusinessException.class);
        time.set(Duration.ofSeconds(16).toNanos());
        response(200, "{\"status\":\"UP\",\"checkoutVersion\":\"2\"}");
        checker.requireCompatible();
        time.set(Duration.ofSeconds(32).toNanos()); response(503, "{}");
        assertThatThrownBy(checker::requireCompatible).hasMessageContaining("Chưa kết nối");
        verify(client, times(3)).send(any(), any());
    }
    @Test void phoneBackendDoesNotRecursivelyProbeItself() {
        checker(true).requireCompatible(); verifyNoInteractions(client);
    }
}
