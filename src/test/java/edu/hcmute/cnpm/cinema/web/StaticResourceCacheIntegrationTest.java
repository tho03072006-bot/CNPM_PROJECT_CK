package edu.hcmute.cnpm.cinema.web;

import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CSS/JS phải được trình duyệt hỏi lại server mỗi lần dùng.
 *
 * Không có Cache-Control thì Chrome tự giữ bản style.css cũ hàng giờ sau khi nhóm sửa giao diện,
 * trang mới chạy với CSS cũ và vỡ bố cục (đã gặp ở trang hóa đơn ngày 02/10/2026).
 */
@AutoConfigureMockMvc
@DisplayName("CSS và JS luôn được tải bản mới nhất")
class StaticResourceCacheIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("style.css trả kèm Cache-Control: no-cache để sửa giao diện xong là trình duyệt thấy ngay")
    void shouldAskBrowserToRevalidateStylesheet() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }
}
