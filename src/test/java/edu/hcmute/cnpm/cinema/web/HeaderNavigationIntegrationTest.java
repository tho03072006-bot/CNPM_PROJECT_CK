package edu.hcmute.cnpm.cinema.web;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HeaderNavigationIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mvc;

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldKeepProfileAndOverflowMenuForEveryRole(Role role) throws Exception {
        var user = testDataFactory.createUserWithRole("header@example.com", role);
        String html = mvc.perform(get("/").sessionAttr(Constants.SESSION_USER, user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String primary = html.substring(html.indexOf("header-primary-nav"), html.indexOf("header-actions"));
        assertThat(primary).contains("class=\"nav-more\"", "Hiện các danh mục khác", "<svg")
                .doesNotContain("Ưu đãi", "Vé của tôi", ">Thêm");
        assertThat(html).contains("account-dropdown", "Hồ sơ của tôi", "Lịch sử đặt vé", "/js/header-nav.js");
        if (role == Role.CUSTOMER) assertThat(primary).contains("Hỗ trợ").doesNotContain("Kho bắp nước", "Soát vé", "Quản trị");
        else assertThat(primary).contains("Kho bắp nước", "Soát vé", "CSKH");
        if (role == Role.ADMIN) assertThat(primary).contains("Quản trị");
        else assertThat(primary).doesNotContain("Quản trị");
    }
}
