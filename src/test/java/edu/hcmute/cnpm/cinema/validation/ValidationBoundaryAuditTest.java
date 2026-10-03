package edu.hcmute.cnpm.cinema.validation;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.SupportConversationRepository;
import edu.hcmute.cnpm.cinema.repository.SupportMessageRepository;
import edu.hcmute.cnpm.cinema.service.CustomerSupportService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Các ca biên bổ sung trong buổi kiểm thử; chỉ chạy trên database _test. */
@AutoConfigureMockMvc
@DisplayName("Rà soát giá trị biên validation toàn project")
class ValidationBoundaryAuditTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired CustomerSupportService support;
    @Autowired SupportConversationRepository conversations;
    @Autowired SupportMessageRepository messages;

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "2147483648", "-2147483649"})
    void malformedHistoryPageIsBadRequest(String page) throws Exception {
        User owner = testDataFactory.createCustomer("boundary-owner@example.com");
        mvc.perform(get("/lich-su-dat-ve").param("page", page).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownHistoryStatusIsBadRequest() throws Exception {
        User owner = testDataFactory.createCustomer("boundary-owner@example.com");
        mvc.perform(get("/lich-su-dat-ve").param("status", "UNKNOWN").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"178956971", "2147483647"})
    void excessiveHistoryPageIsBadRequest(String page) throws Exception {
        User owner = testDataFactory.createCustomer("boundary-owner@example.com");
        mvc.perform(get("/lich-su-dat-ve").param("page", page).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Số trang lịch sử không hợp lệ")));
    }

    @Test
    void largestSupportedHistoryOffsetStillShowsEmptyPage() throws Exception {
        User owner = testDataFactory.createCustomer("boundary-owner@example.com");
        mvc.perform(get("/lich-su-dat-ve").param("page", "178956970")
                        .sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trang này chưa có giao dịch")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2026-02-30", "2026-13-01"})
    void malformedDayIsBadRequest(String date) throws Exception {
        User staff = testDataFactory.createUserWithRole("boundary-staff@example.com", Role.STAFF);
        mvc.perform(get("/nhan-vien/suat-chieu").param("ngay", date).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000-01-01", "+10000-01-01", "+999999999-12-31"})
    void dayOutsideDatabaseRangeIsBadRequest(String date) throws Exception {
        User staff = testDataFactory.createUserWithRole("boundary-staff@example.com", Role.STAFF);
        mvc.perform(get("/nhan-vien/suat-chieu").param("ngay", date).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ngày xem lịch không hợp lệ")));
    }

    @Test
    void lastDatabaseDayDoesNotOverflowQueryEnd() throws Exception {
        User staff = testDataFactory.createUserWithRole("boundary-staff@example.com", Role.STAFF);
        LocalDate lastDay = LocalDate.of(9999, 12, 31);
        Room room = testDataFactory.createRoom("Cinema ngày cuối", 1, 2);
        testDataFactory.createShowtime(testDataFactory.createMovie("Phim ngày cuối"), room, lastDay.atTime(12, 0));
        testDataFactory.createShowtime(testDataFactory.createMovie("Phim hôm trước"), room,
                lastDay.minusDays(1).atTime(23, 0));
        mvc.perform(get("/nhan-vien/suat-chieu").param("ngay", "9999-12-31")
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim ngày cuối")))
                .andExpect(content().string(not(containsString("Phim hôm trước"))))
                .andExpect(content().string(not(containsString("Hôm sau →"))))
                .andExpect(model().attribute("nextDate", nullValue()));
    }

    @Test
    void firstDatabaseDayHasInputBoundsAndNoPreviousDayLink() throws Exception {
        User staff = testDataFactory.createUserWithRole("boundary-staff@example.com", Role.STAFF);
        mvc.perform(get("/nhan-vien/suat-chieu").param("ngay", "0001-01-01")
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("min=\"0001-01-01\"")))
                .andExpect(content().string(containsString("max=\"9999-12-31\"")))
                .andExpect(content().string(not(containsString("← Hôm trước"))))
                .andExpect(model().attribute("previousDate", nullValue()));
    }

    static Stream<Arguments> invalidMovieFields() {
        return Stream.of(Arguments.of("title", " "), Arguments.of("title", "a".repeat(201)),
                Arguments.of("genre", "a".repeat(101)), Arguments.of("posterUrl", "a".repeat(501)),
                Arguments.of("durationMin", "0"), Arguments.of("durationMin", "abc"),
                Arguments.of("ageRating", "INVALID"));
    }

    @ParameterizedTest
    @MethodSource("invalidMovieFields")
    void invalidMovieFormIsRedisplayedWithoutSaving(String field, String value) throws Exception {
        User admin = testDataFactory.createUserWithRole("boundary-admin@example.com", Role.ADMIN);
        long before = movieRepository.count();
        mvc.perform(post("/admin/movies").sessionAttr(Constants.SESSION_USER, admin)
                        .param("title", field.equals("title") ? value : "Phim kiểm thử")
                        .param("genre", field.equals("genre") ? value : "Hoạt hình")
                        .param("durationMin", field.equals("durationMin") ? value : "120")
                        .param("posterUrl", field.equals("posterUrl") ? value : "")
                        .param("ageRating", field.equals("ageRating") ? value : "P"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("movieForm", field));
        assertThat(movieRepository.count()).isEqualTo(before);
    }

    static Stream<Arguments> invalidRoomFields() {
        return Stream.of(Arguments.of("name", " "), Arguments.of("name", "a".repeat(51)),
                Arguments.of("totalRows", "0"), Arguments.of("totalRows", "27"),
                Arguments.of("totalRows", "abc"), Arguments.of("totalColumns", "0"),
                Arguments.of("totalColumns", "51"), Arguments.of("totalColumns", "1.5"));
    }

    @ParameterizedTest
    @MethodSource("invalidRoomFields")
    void invalidRoomFormIsRedisplayedWithoutSaving(String field, String value) throws Exception {
        User admin = testDataFactory.createUserWithRole("boundary-admin@example.com", Role.ADMIN);
        long before = roomRepository.count();
        mvc.perform(post("/admin/rooms").sessionAttr(Constants.SESSION_USER, admin)
                        .param("name", field.equals("name") ? value : "Cinema kiểm thử")
                        .param("totalRows", field.equals("totalRows") ? value : "5")
                        .param("totalColumns", field.equals("totalColumns") ? value : "8"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("roomForm", field));
        assertThat(roomRepository.count()).isEqualTo(before);
        assertThat(seatRepository.count()).isZero();
    }

    static Stream<Arguments> invalidSupportFields() {
        return Stream.of(Arguments.of("subject", "abcd"), Arguments.of("subject", "a".repeat(201)),
                Arguments.of("content", " "), Arguments.of("content", "a".repeat(9)),
                Arguments.of("content", "a".repeat(2001)), Arguments.of("category", ""));
    }

    @ParameterizedTest
    @MethodSource("invalidSupportFields")
    void invalidSupportRequestHasNoPartialWrites(String field, String value) {
        User owner = testDataFactory.createCustomer("boundary-support@example.com");
        assertThatThrownBy(() -> support.create(owner.getId(), field.equals("subject") ? value : "Cần hỗ trợ vé",
                field.equals("category") ? null : SupportCategory.BOOKING,
                field.equals("content") ? value : "Tôi muốn kiểm tra lại vé đã đặt."))
                .isInstanceOf(BusinessException.class);
        assertThat(conversations.count()).isZero();
        assertThat(messages.count()).isZero();
    }

    @Test
    void supportAcceptsExactUnicodeLengthLimits() {
        User owner = testDataFactory.createCustomer("boundary-support@example.com");
        var conversation = support.create(owner.getId(), "ế".repeat(200), SupportCategory.BOOKING, "ế".repeat(2000));
        var saved = support.findForCustomer(conversation.getId(), owner.getId());
        assertThat(saved.getSubject()).hasSize(200);
        assertThat(saved.getMessages().getFirst().getContent()).isEqualTo("ế".repeat(2000));
    }

    @Test
    void customerCannotReplyToClosedSupportConversation() {
        User owner = testDataFactory.createCustomer("boundary-support@example.com");
        User staff = testDataFactory.createUserWithRole("boundary-staff@example.com", Role.STAFF);
        var conversation = support.create(owner.getId(), "Cần hỗ trợ vé", SupportCategory.BOOKING,
                "Tôi muốn kiểm tra lại vé đã đặt.");
        support.resolve(conversation.getId(), staff.getId());
        assertThatThrownBy(() -> support.replyAsCustomer(conversation.getId(), owner.getId(), "Cần kiểm tra tiếp"))
                .isInstanceOf(BusinessException.class);
        assertThat(messages.count()).isEqualTo(1);
    }
}
