package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.config.MomoProperties;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gọi API của MoMo qua HTTP: tạo yêu cầu thanh toán và hoàn tiền.
 *
 * Chỉ lo đúng phần "nói chuyện với MoMo". Tách riêng để test thay lớp này bằng bản giả -
 * bộ test không bao giờ gọi MoMo thật.
 */
@Component
public class MomoApiClient {

    private static final Logger log = LoggerFactory.getLogger(MomoApiClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

    private final MomoProperties properties;
    private final RestClient restClient;

    public MomoApiClient(MomoProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Tạo yêu cầu thanh toán, trả về địa chỉ trang thanh toán của MoMo để chuyển khách sang.
     *
     * @param orderId mã đơn, phải khác nhau cho mỗi lần tạo
     * @param amount  số tiền, đơn vị đồng
     */
    public String createPayment(String orderId, long amount, String orderInfo) {
        String extraData = "";
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + properties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + properties.getRedirectUrl()
                + "&requestId=" + orderId
                + "&requestType=" + properties.getRequestType();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("requestId", orderId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", properties.getRedirectUrl());
        body.put("ipnUrl", properties.getIpnUrl());
        body.put("requestType", properties.getRequestType());
        body.put("extraData", extraData);
        body.put("lang", "vi");
        body.put("signature", properties.sign(rawSignature));

        Map<String, Object> response = post("/v2/gateway/api/create", body);
        if (!isSuccess(response) || response.get("payUrl") == null) {
            throw new BusinessException("MoMo chưa tạo được giao dịch: " + response.get("message")
                    + ". Bạn thử lại hoặc chọn trả tại quầy.");
        }
        return String.valueOf(response.get("payUrl"));
    }

    /**
     * Hoàn một phần hoặc toàn bộ tiền của một giao dịch đã thanh toán.
     *
     * @param refundOrderId mã riêng cho lần hoàn này (MoMo bắt mỗi lần hoàn một mã khác)
     * @param transId       mã giao dịch MoMo trả về lúc khách thanh toán
     * @return mã giao dịch hoàn tiền bên MoMo
     */
    public String refund(String refundOrderId, String transId, long amount, String description) {
        String rawSignature = "accessKey=" + properties.getAccessKey()
                + "&amount=" + amount
                + "&description=" + description
                + "&orderId=" + refundOrderId
                + "&partnerCode=" + properties.getPartnerCode()
                + "&requestId=" + refundOrderId
                + "&transId=" + transId;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("orderId", refundOrderId);
        body.put("requestId", refundOrderId);
        body.put("amount", amount);
        body.put("transId", Long.parseLong(transId));
        body.put("lang", "vi");
        body.put("description", description);
        body.put("signature", properties.sign(rawSignature));

        Map<String, Object> response = post("/v2/gateway/api/refund", body);
        if (!isSuccess(response)) {
            throw new BusinessException("MoMo chưa hoàn được tiền: " + response.get("message")
                    + ". Vé chưa bị huỷ, bạn thử lại sau.");
        }
        return String.valueOf(response.get("transId"));
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.getEndpoint() + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);
            log.info("MoMo {} -> resultCode={} message={}", path,
                    response == null ? null : response.get("resultCode"),
                    response == null ? null : response.get("message"));
            return response == null ? Map.of() : response;
        } catch (RestClientException exception) {
            log.warn("Goi MoMo {} that bai: {}", path, exception.getMessage());
            throw new BusinessException("Không kết nối được tới MoMo. Bạn thử lại sau ít phút.");
        }
    }

    private boolean isSuccess(Map<String, Object> response) {
        Object resultCode = response.get("resultCode");
        return resultCode != null && "0".equals(String.valueOf(resultCode));
    }
}
