package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult.Verdict;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketLookupService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Soát vé cho khách vào phòng và chặn một vé dùng hai lần. */
@AutoConfigureMockMvc
@DisplayName("Ghi nhận khách đã vào phòng")
class CheckInIntegrationTest extends IntegrationTestBase {

    private static final LocalDate SHOW_DAY = LocalDate.now().plusDays(2);
    private static final LocalDateTime SAME_DAY_BEFORE_START = SHOW_DAY.atTime(18, 30);

    @Autowired
    private TicketLookupService ticketLookupService;
    @Autowired private edu.hcmute.cnpm.cinema.service.TicketCodeService codes;
    @Autowired
    private MockMvc mockMvc;

    private Ticket paidTicket;
    private Ticket heldTicket;
    private Movie movie;
    private Room room;
    private User customer;

    @BeforeEach
    void setUpTickets() {
        movie = testDataFactory.createMovie("Phim vào phòng");
        room = testDataFactory.createRoom("Cinema 5", 1, 4);
        Seat seatA1 = testDataFactory.createSeat(room, "A", 1);
        Seat seatA2 = testDataFactory.createSeat(room, "A", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room, SHOW_DAY.atTime(19, 0));
        customer = testDataFactory.createCustomer("khach.vaophong@example.com");
        paidTicket = savePaid(testDataFactory.newHeldTicket(showtime, seatA1, customer));
        heldTicket = ticketRepository.save(testDataFactory.newHeldTicket(showtime, seatA2, customer));
    }

    @Test
    @DisplayName("Cho vào xong thì soát lại cùng mã vé báo ĐÃ VÀO PHÒNG, không cho vào lần nữa")
    void shouldReportCheckedIn_whenScannedAgainAfterEntry() {
        ticketLookupService.checkIn(paidTicket.getId(), SAME_DAY_BEFORE_START);

        var rescan = ticketLookupService.checkTicketCode(codes.codeFor(paidTicket.getId()), SAME_DAY_BEFORE_START.plusMinutes(5));
        assertThat(rescan.getVerdict()).isEqualTo(Verdict.CHECKED_IN);
        assertThat(ticketRepository.findById(paidTicket.getId()).orElseThrow().getCheckedInAt())
                .isEqualTo(SAME_DAY_BEFORE_START);
    }

    @Test
    @DisplayName("Bấm Cho vào lần thứ hai thì bị từ chối")
    void shouldRejectSecondCheckIn() {
        ticketLookupService.checkIn(paidTicket.getId(), SAME_DAY_BEFORE_START);

        assertThatThrownBy(() -> ticketLookupService.checkIn(paidTicket.getId(), SAME_DAY_BEFORE_START.plusMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được soát vào phòng");
    }

    @Test
    @DisplayName("Vé chưa thanh toán hoặc chưa tới ngày thì không cho vào")
    void shouldRejectCheckIn_whenTicketNotValid() {
        assertThatThrownBy(() -> ticketLookupService.checkIn(heldTicket.getId(), SAME_DAY_BEFORE_START))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ticketLookupService.checkIn(paidTicket.getId(), SHOW_DAY.minusDays(1).atTime(19, 0)))
                .isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findById(paidTicket.getId()).orElseThrow().getCheckedInAt()).isNull();
    }

    @Test
    @DisplayName("Nhân viên bấm Cho vào trên trang thì ghi giờ vào phòng rồi quay lại kết quả soát")
    void shouldCheckInThroughStaffPage() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien@example.com", Role.STAFF);
        // Suất đang chiếu ngay lúc chạy test: bắt đầu từ đầu giờ hiện tại, dài 120 phút,
        // nên luôn "đúng ngày và chưa kết thúc" dù chạy test lúc mấy giờ.
        LocalDateTime startOfThisHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        Showtime runningNow = testDataFactory.createShowtime(movie, room, startOfThisHour);
        Seat seatA3 = testDataFactory.createSeat(room, "A", 3);
        Ticket ticket = savePaid(testDataFactory.newHeldTicket(runningNow, seatA3, customer));

        mockMvc.perform(post("/nhan-vien/soat-ve/{id}/vao-phong", ticket.getId())
                        .sessionAttr(edu.hcmute.cnpm.cinema.controller.StaffCheckInSecurity.SESSION_KEY, "a".repeat(64))
                        .param("checkInCsrf", "a".repeat(64))
                        .param("ma", codes.codeFor(ticket.getId()))
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nhan-vien/soat-ve?ma=" + codes.codeFor(ticket.getId())));

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getCheckedInAt()).isNotNull();
    }

    @Test
    @DisplayName("Khách hàng không tự bấm Cho vào được")
    void shouldForbidCheckIn_whenCustomerCallsUrl() throws Exception {
        mockMvc.perform(post("/nhan-vien/soat-ve/{id}/vao-phong", paidTicket.getId())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
        assertThat(ticketRepository.findById(paidTicket.getId()).orElseThrow().getCheckedInAt()).isNull();
    }

    private Ticket savePaid(Ticket ticket) {
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}
