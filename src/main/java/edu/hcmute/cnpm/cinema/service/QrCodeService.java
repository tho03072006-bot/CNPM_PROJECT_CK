package edu.hcmute.cnpm.cinema.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Vẽ mã QR thành SVG để nhúng thẳng vào trang.
 *
 * SVG không ghi mã màu: các ô đen tô bằng {@code currentColor}, nền để trống, nên màu do
 * CSS quyết định (biến --qr-fg / --qr-bg). Mã QR phải luôn đen trên trắng ở cả chế độ tối,
 * nếu không nhiều app quét không đọc được.
 */
@Service
public class QrCodeService {

    /** Vùng trắng quanh mã, đơn vị là số ô. Chuẩn QR yêu cầu tối thiểu 4. */
    private static final int QUIET_ZONE_MODULES = 4;

    public String toSvg(String content, String accessibleLabel) {
        BitMatrix matrix = encode(content);
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        // Gộp các ô đen liền nhau trên cùng một dòng thành một đoạn, SVG nhẹ hơn nhiều lần.
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                if (!matrix.get(x, y)) {
                    x++;
                    continue;
                }
                int runStart = x;
                while (x < width && matrix.get(x, y)) {
                    x++;
                }
                path.append('M').append(runStart).append(' ').append(y)
                        .append('h').append(x - runStart).append("v1h-").append(x - runStart).append('z');
            }
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"qr-svg\" viewBox=\"0 0 " + width + " " + height
                + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"" + escapeAttribute(accessibleLabel) + "\">"
                + "<path fill=\"currentColor\" d=\"" + path + "\"/></svg>";
    }

    private BitMatrix encode(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Nội dung mã QR đang trống.");
        }
        try {
            // Kích thước 0 nghĩa là lấy đúng một điểm cho mỗi ô; phóng to là việc của SVG.
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                    EncodeHintType.MARGIN, QUIET_ZONE_MODULES));
        } catch (WriterException exception) {
            throw new IllegalStateException("Không tạo được mã QR.", exception);
        }
    }

    private static String escapeAttribute(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
