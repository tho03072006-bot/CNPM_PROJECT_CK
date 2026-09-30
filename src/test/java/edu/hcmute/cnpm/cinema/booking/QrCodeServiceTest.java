package edu.hcmute.cnpm.cinema.booking;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import edu.hcmute.cnpm.cinema.service.QrCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Vẽ mã QR thanh toán")
class QrCodeServiceTest {

    private static final Pattern VIEW_BOX = Pattern.compile("viewBox=\"0 0 (\\d+) (\\d+)\"");
    private static final Pattern RUN = Pattern.compile("M(\\d+) (\\d+)h(\\d+)v1h-\\d+z");
    /** Phóng mỗi ô lên vài điểm ảnh cho bộ đọc QR dễ đọc, giống như trên màn hình thật. */
    private static final int PIXELS_PER_MODULE = 4;

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    @DisplayName("Mã QR vẽ ra đọc ngược lại được đúng nội dung MoMo trả về")
    void shouldEncodeContentThatDecodesBack_whenRenderingSvg() throws Exception {
        String content = "momo://app?action=payWithApp&isScanQR=true&serviceType=qr&sid=TU9NT3xVVEU&v=3.0";

        String svg = qrCodeService.toSvg(content, "Mã QR thanh toán");

        assertThat(decode(svg)).isEqualTo(content);
    }

    @Test
    @DisplayName("SVG không ghi mã màu, để CSS giữ mã QR đen trên trắng ở cả chế độ tối")
    void shouldUseCurrentColorOnly_whenRenderingSvg() {
        String svg = qrCodeService.toSvg("UTE Cinema", "Mã QR \"thử\"");

        assertThat(svg).contains("fill=\"currentColor\"").doesNotContain("#");
        assertThat(svg).contains("aria-label=\"Mã QR &quot;thử&quot;\"");
    }

    @Test
    @DisplayName("Không vẽ mã QR rỗng")
    void shouldReject_whenContentIsBlank() {
        assertThatThrownBy(() -> qrCodeService.toSvg(" ", "Mã QR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Dựng lại ảnh từ các đoạn trong SVG rồi cho bộ đọc QR của ZXing đọc. */
    private String decode(String svg) throws Exception {
        Matcher viewBox = VIEW_BOX.matcher(svg);
        assertThat(viewBox.find()).isTrue();
        int width = Integer.parseInt(viewBox.group(1));
        int height = Integer.parseInt(viewBox.group(2));

        boolean[][] dark = new boolean[height][width];
        Matcher run = RUN.matcher(svg);
        while (run.find()) {
            int x = Integer.parseInt(run.group(1));
            int y = Integer.parseInt(run.group(2));
            int length = Integer.parseInt(run.group(3));
            for (int offset = 0; offset < length; offset++) {
                dark[y][x + offset] = true;
            }
        }

        int imageWidth = width * PIXELS_PER_MODULE;
        int imageHeight = height * PIXELS_PER_MODULE;
        int[] pixels = new int[imageWidth * imageHeight];
        for (int py = 0; py < imageHeight; py++) {
            for (int px = 0; px < imageWidth; px++) {
                boolean isDark = dark[py / PIXELS_PER_MODULE][px / PIXELS_PER_MODULE];
                pixels[py * imageWidth + px] = isDark ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(imageWidth, imageHeight, pixels)));
        return new QRCodeReader().decode(bitmap).getText();
    }
}
