package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.CustomerSupportService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Chăm sóc khách hàng")
class CustomerSupportIntegrationTest extends IntegrationTestBase {

    @Autowired private CustomerSupportService supportService;
    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Khách gửi yêu cầu, nhân viên trả lời và khách khác không đọc được")
    void shouldKeepConversationPrivate_whenStaffReplies() throws Exception {
        User customer = testDataFactory.createCustomer("support@example.com");
        User otherCustomer = testDataFactory.createCustomer("other@example.com");
        User staff = testDataFactory.createUserWithRole("staff@example.com", Role.STAFF);

        SupportConversation conversation = supportService.create(customer.getId(),
                "Chưa thấy vé sau thanh toán", SupportCategory.PAYMENT,
                "Tôi đã thanh toán nhưng cần rạp kiểm tra lại hóa đơn.");
        supportService.replyAsStaff(conversation.getId(), staff.getId(),
                "Rạp đã kiểm tra và vé của bạn đã được ghi nhận.");

        SupportConversation reloaded = supportService.findForCustomer(conversation.getId(), customer.getId());
        assertThat(reloaded.getStatus()).isEqualTo(SupportStatus.WAITING_CUSTOMER);
        assertThat(reloaded.getMessages()).hasSize(2);
        assertThatThrownBy(() -> supportService.findForCustomer(conversation.getId(), otherCustomer.getId()))
                .isInstanceOf(BusinessException.class);

        mockMvc.perform(get("/nhan-vien/ho-tro/{id}", conversation.getId())
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rạp đã kiểm tra")));
    }

    @Test
    @DisplayName("Thanh điều hướng báo số yêu cầu đang chờ nhân viên và đánh dấu mục hiện tại")
    void shouldShowPendingBadgeAndActiveNavigation_forStaff() throws Exception {
        User customer = testDataFactory.createCustomer("badge-customer@example.com");
        User staff = testDataFactory.createUserWithRole("badge-staff@example.com", Role.STAFF);
        supportService.create(customer.getId(), "Cần kiểm tra vé điện tử",
                SupportCategory.BOOKING, "Tôi cần nhân viên hỗ trợ kiểm tra lại thông tin vé.");

        mockMvc.perform(get("/nhan-vien/ho-tro").sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("nav-cskh is-active")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("nav-notification-badge")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">1</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhân viên rạp")));
    }

    @Test
    @DisplayName("Tách đúng hộp thư khách hàng và hộp thư nhân viên")
    void shouldSeparateCustomerAndStaffSupportAreas() throws Exception {
        User customer = testDataFactory.createCustomer("chat-customer@example.com");
        User staff = testDataFactory.createUserWithRole("chat-staff@example.com", Role.STAFF);
        SupportConversation conversation = supportService.create(customer.getId(), "Cần hỗ trợ đổi suất chiếu",
                SupportCategory.BOOKING, "Tôi muốn hỏi cách đổi sang một suất chiếu khác.");

        mockMvc.perform(get("/ho-tro/{id}", conversation.getId())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("support-chat-shell")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is-own")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hỗ trợ</a>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is-active")));

        mockMvc.perform(get("/ho-tro/{id}", conversation.getId())
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/nhan-vien/ho-tro/{id}", conversation.getId())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
    }
}
