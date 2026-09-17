package edu.hcmute.cnpm.cinema.exception;

import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiem tra {@link GlobalExceptionHandler} that su lam viec: loi nghiep vu nem tu tang Service
 * phai ra trang bao loi tu te (hoac JSON neu la AJAX), KHONG duoc de loi 500 tho
 * - day la mot muc bat buoc trong checklist duyet PR o CONTRIBUTING.md.
 *
 * Test dung 1 Controller gia (chi ton tai trong test) chuyen nem loi, de khong phai cho
 * Controller that cua 3 module kia code xong.
 *
 * Phu trach: Tho (Module 4 - Testing).
 */
@AutoConfigureMockMvc
@DisplayName("Bat loi tap trung - GlobalExceptionHandler")
class GlobalExceptionHandlerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Ghe da co nguoi giu -> tra ve trang loi voi ma 409")
    void shouldReturnErrorPageWithConflictStatus_whenSeatAlreadyTaken() throws Exception {
        mockMvc.perform(get("/test-loi/ghe-da-co-nguoi-giu"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorCode", 409))
                .andExpect(model().attribute("errorMessage", containsString("chon ghe khac")))
                // thong bao phai that su duoc in ra HTML qua fragment alert trong layout chung
                .andExpect(content().string(containsString("alert-error")))
                .andExpect(content().string(containsString("chon ghe khac")));
    }

    @Test
    @DisplayName("Request AJAX -> tra ve JSON thay vi trang HTML")
    void shouldReturnJsonBody_whenRequestComesFromAjax() throws Exception {
        mockMvc.perform(get("/test-loi/ghe-da-co-nguoi-giu")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("chon ghe khac")));
    }

    @Test
    @DisplayName("Khong tim thay du lieu -> tra ve trang loi voi ma 404")
    void shouldReturnErrorPageWithNotFoundStatus_whenResourceIsMissing() throws Exception {
        mockMvc.perform(get("/test-loi/khong-tim-thay"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", containsString("Khong tim thay phim")));
    }

    @Test
    @DisplayName("Dat ve khong hop le -> tra ve trang loi voi ma 400")
    void shouldReturnErrorPageWithBadRequestStatus_whenBookingIsInvalid() throws Exception {
        mockMvc.perform(get("/test-loi/dat-ve-khong-hop-le"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorCode", 400));
    }

    @TestConfiguration
    static class ThrowingControllerConfiguration {

        @Bean
        ThrowingController throwingController() {
            return new ThrowingController();
        }
    }

    /** Controller gia chi dung trong test, khong nam trong ma nguon chay that. */
    @Controller
    @RequestMapping("/test-loi")
    static class ThrowingController {

        @GetMapping("/ghe-da-co-nguoi-giu")
        String throwSeatAlreadyTaken() {
            throw new SeatAlreadyTakenException(1L, 2L);
        }

        @GetMapping("/khong-tim-thay")
        String throwResourceNotFound() {
            throw new ResourceNotFoundException("phim", 99L);
        }

        @GetMapping("/dat-ve-khong-hop-le")
        String throwInvalidBooking() {
            throw new InvalidBookingException("Suat chieu nay da bat dau, ban khong the dat ve nua.");
        }
    }
}
