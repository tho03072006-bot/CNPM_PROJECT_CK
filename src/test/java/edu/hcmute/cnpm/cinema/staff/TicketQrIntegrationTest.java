package edu.hcmute.cnpm.cinema.staff;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.StaffCheckInSecurity;
import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult.Verdict;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TicketQrIntegrationTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired TicketLookupService lookup;
    @Autowired TicketCodeService codes;
    @Autowired BookingClock clock;
    @Autowired PaymentService payment;
    @Autowired ReceiptService receipts;
    @Autowired TicketRefundService refunds;
    @Autowired StaffCheckInSecurity security;
    User owner, staff, stranger;
    Movie movie;
    Showtime show;
    List<Ticket> paid;

    @BeforeEach void fixture() {
        owner = testDataFactory.createCustomer("qr-owner@example.com");
        staff = testDataFactory.createUserWithRole("qr-staff@example.com", Role.STAFF);
        stranger = testDataFactory.createCustomer("qr-stranger@example.com");
        movie = testDataFactory.createMovie("QR admission movie");
        Room room = testDataFactory.createRoom("Cinema QR", 1, 3);
        // Thanh toán hợp lệ trước giờ chiếu; ngày hiện tại để có thể soát vé.
        show = testDataFactory.createShowtime(movie, room, clock.now().plusMinutes(60));
        for (int column = 1; column <= 2; column++)
            ticketRepository.save(testDataFactory.newHeldTicket(show, testDataFactory.createSeat(room, "A", column), owner));
        paid = payment.confirmPayment(owner.getId(), show.getId());
    }

    @Test void qrImageAndNumericCodeIdentifyExactlySameTicket() throws Exception {
        for (Ticket ticket : paid) {
            byte[] png = mvc.perform(get("/ve/{id}/qr.png", ticket.getId()).sessionAttr(Constants.SESSION_USER, owner))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andReturn().getResponse().getContentAsByteArray();
            var image = ImageIO.read(new ByteArrayInputStream(png));
            int w = image.getWidth(), h = image.getHeight();
            String payload = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(
                    new RGBLuminanceSource(w, h, image.getRGB(0, 0, w, h, null, 0, w))))).getText();
            assertThat(payload).isEqualTo(codes.payload(codes.codeFor(ticket.getId())));
            assertThat(lookup.checkTicketCode(payload, show.getStartTime()).getTicket().getId()).isEqualTo(ticket.getId());
            assertThat(lookup.checkTicketCode(codes.codeFor(ticket.getId()), show.getStartTime()).getVerdict()).isEqualTo(Verdict.VALID);
        }
    }

    @Test void invoiceShowsOneSharedQrAndDistinctCodeForEverySeat() throws Exception {
        String code = paid.getFirst().getBookingOrder().getReceiptCode();
        var receipt = receipts.findReceipt(code, owner);
        assertThat(receipt.lines()).hasSize(2);
        assertThat(receipt.lines()).extracting(l -> l.ticketCode()).doesNotHaveDuplicates();
        assertThat(receipt.admissionTickets()).hasSize(2);
        assertThat(receipt.admissionPayload()).isEqualTo(codes.bookingPayload(code));
        assertThat(receipt.admissionQrSvg()).contains("QR chung 2 ghế");
        assertThat(receipt.lines()).allSatisfy(l -> assertThat(l.qrSvg()).isNull());
        String html = mvc.perform(get("/hoa-don/{code}", code).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (Ticket t : paid) assertThat(html).contains(codes.codeFor(t.getId()));
        assertThat(html).contains("Thanh toán thành công", "In hóa đơn và vé");
    }

    @Test void imageRequiresOwnerOrStaffAndPaidTicket() throws Exception {
        Long id = paid.getFirst().getId();
        mvc.perform(get("/ve/{id}/qr.png", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/ve/{id}/qr.png", id).sessionAttr(Constants.SESSION_USER, stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/ve/{id}/qr.png", id).sessionAttr(Constants.SESSION_USER, staff)).andExpect(status().isOk());
        Ticket held = ticketRepository.save(testDataFactory.newHeldTicket(show,
                testDataFactory.createSeat(show.getRoom(), "A", 3), owner));
        mvc.perform(get("/ve/{id}/qr.png", held.getId()).sessionAttr(Constants.SESSION_USER, owner)).andExpect(status().isNotFound());
    }

    @Test void scanOnlyLooksUpAndExplicitConfirmationMakesTicketUsed() throws Exception {
        Ticket ticket = paid.getFirst();
        MockHttpSession session = new MockHttpSession(); session.setAttribute(Constants.SESSION_USER, staff);
        mvc.perform(get("/nhan-vien/soat-ve").param("ma", codes.payload(codes.codeFor(ticket.getId()))).session(session))
                .andExpect(status().isOk()).andExpect(model().attributeExists("result", "checkInCsrf"));
        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getCheckedInAt()).isNull();
        mvc.perform(post("/nhan-vien/soat-ve/{id}/vao-phong", ticket.getId()).session(session)
                .param("checkInCsrf", security.token(session)).param("ma", codes.codeFor(ticket.getId()))).andExpect(status().is3xxRedirection());
        assertThat(lookup.checkTicketCode(codes.payload(codes.codeFor(ticket.getId()))).getVerdict()).isEqualTo(Verdict.CHECKED_IN);
        assertThatThrownBy(() -> lookup.checkIn(ticket.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void forgedConfirmationCannotConsumeTicket() throws Exception {
        Long id = paid.getFirst().getId();
        MockHttpSession session = new MockHttpSession(); session.setAttribute(Constants.SESSION_USER, staff);
        security.token(session);
        for (String csrf : new String[]{"", "wrong", "b".repeat(64)}) {
            mvc.perform(post("/nhan-vien/soat-ve/{id}/vao-phong", id).session(session).param("checkInCsrf", csrf))
                    .andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));
            assertThat(ticketRepository.findById(id).orElseThrow().getCheckedInAt()).isNull();
        }
    }

    @Test void twoDoorsAdmittingConcurrentlyHaveExactlyOneWinner() throws Exception {
        Long id = paid.getFirst().getId();
        LocalDateTime now = show.getStartTime().minusMinutes(5);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> admit = () -> { start.await(); try { lookup.checkIn(id, now); return true; }
                catch (BusinessException error) { return false; } };
            Future<Boolean> a = pool.submit(admit), b = pool.submit(admit); start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test void inactiveMovieAndWrongRoomCannotBeAdmitted() {
        Long id = paid.getFirst().getId();
        movie.setActive(false); movieRepository.saveAndFlush(movie);
        assertThat(lookup.checkTicketCode(codes.codeFor(id)).getVerdict()).isEqualTo(Verdict.INVALID);
        assertThatThrownBy(() -> lookup.checkIn(id)).isInstanceOf(BusinessException.class);
        movie.setActive(true); movieRepository.saveAndFlush(movie);
        Room other = testDataFactory.createRoom("Cinema other", 1, 1);
        jdbcTemplate.update("UPDATE showtimes SET room_id = ? WHERE id = ?", other.getId(), show.getId());
        assertThat(lookup.checkTicketCode(codes.codeFor(id)).getVerdict()).isEqualTo(Verdict.INVALID);
        assertThatThrownBy(() -> lookup.checkIn(id)).isInstanceOf(BusinessException.class);
    }

    @Test void refundedTicketHasHistoricalNumberButNoAdmissionQr() {
        // Hoàn vé đòi trước suất chiếu ít nhất 24 giờ.
        show.setStartTime(clock.now().plusDays(3)); show.setEndTime(show.getStartTime().plusHours(2)); showtimeRepository.saveAndFlush(show);
        Ticket cancelled = paid.getFirst();
        String invoice = cancelled.getBookingOrder().getReceiptCode();
        refunds.cancelPaidTicket(owner.getId(), cancelled.getId(), clock.now());
        var receipt = receipts.findReceipt(invoice, owner);
        assertThat(receipt.lines()).filteredOn(l -> l.refunded()).singleElement().satisfies(l -> {
            assertThat(l.ticketCode()).isEqualTo(codes.codeFor(cancelled.getId())); assertThat(l.qrSvg()).isNull();
        });
        assertThatThrownBy(() -> lookup.checkTicketCode(codes.payload(codes.codeFor(cancelled.getId())))).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsMalformedZeroOverflowAndPaymentQr() {
        for (String bad : new String[]{"0", "-1", "1.0", "9223372036854775808", "9".repeat(1000), "https://wallet/pay/1", "UTE-CINEMA:TICKET:0"})
            assertThatThrownBy(() -> lookup.checkTicketCode(bad)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> lookup.checkIn(0L)).isInstanceOf(BusinessException.class);
        assertThat(codes.parse("  #12345678 ")).isEqualTo("12345678");
        assertThatThrownBy(() -> codes.parse("12")).isInstanceOf(BusinessException.class);
    }

    @Test void invalidEmailAndNonexistentCodeAreHandledWithoutAdmitting() throws Exception {
        for (String email : new String[]{"not-email", "a@@example.com", " ", "a".repeat(151) + "@example.com"})
            assertThatThrownBy(() -> lookup.findUpcomingTicketsByEmail(email)).isInstanceOf(BusinessException.class);
        mvc.perform(get("/nhan-vien/soat-ve").sessionAttr(Constants.SESSION_USER, staff).param("ma", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isOk()).andExpect(model().attributeExists(Constants.MODEL_ERROR_MESSAGE));
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }

    @Test void refundFromAnotherPaymentInSameSecondDoesNotAppearOnInvoice() {
        show.setStartTime(clock.now().plusDays(3)); show.setEndTime(show.getStartTime().plusHours(2)); showtimeRepository.saveAndFlush(show);
        Ticket cancelled = paid.getFirst(); String code = cancelled.getBookingOrder().getReceiptCode();
        var refund = refunds.cancelPaidTicket(owner.getId(), cancelled.getId(), clock.now());
        // Hai lượt thanh toán có thể cùng giây. Chỉ chứng từ đúng thời điểm được ghép.
        refund.setBookingOrderId(null); // Chứng từ lịch sử chưa ghi hóa đơn gốc.
        refund.setPaidAt(refund.getPaidAt().plusNanos(200_000_000)); ticketRefundRepository.saveAndFlush(refund);
        assertThat(receipts.findReceipt(code, owner).lines()).noneMatch(l -> l.refunded());
    }
    private String invoice() { return paid.getFirst().getBookingOrder().getReceiptCode(); }
    private String bundle() { return codes.bookingPayload(invoice()); }
    private Ticket buyThirdSeat() {
        var room = show.getRoom();
        ticketRepository.save(testDataFactory.newHeldTicket(show, testDataFactory.createSeat(room, "A", 3), owner));
        return payment.confirmPayment(owner.getId(), show.getId()).getFirst();
    }
    @Test void sharedQrListsExactlyTheSeatsOfThatInvoice() throws Exception {
        Ticket separate = buyThirdSeat();
        var result = lookup.checkBookingQr(bundle());
        assertThat(result.tickets()).extracting(t -> t.getTicket().getId())
                .containsExactlyInAnyOrderElementsOf(paid.stream().map(Ticket::getId).toList());
        assertThat(result.tickets()).extracting(t -> t.getTicketCode()).doesNotHaveDuplicates();
        assertThat(result.validCount()).isEqualTo(2);
        mvc.perform(get("/nhan-vien/soat-ve").param("qr", bundle()).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk()).andExpect(model().attributeExists("bookingResult"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("QR hóa đơn")));
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
        assertThat(lookup.checkInBooking(invoice(), bundle())).isEqualTo(2);
        assertThat(ticketRepository.findById(separate.getId()).orElseThrow().getCheckedInAt()).isNull();
    }
    @Test void oneSeatInvoiceKeepsIndividualQrAndEightDigitCode() {
        Ticket one = buyThirdSeat();
        var receipt = receipts.findReceipt(one.getBookingOrder().getReceiptCode(), owner);
        assertThat(receipt.admissionTickets()).hasSize(1);
        assertThat(receipt.admissionPayload()).isEqualTo(codes.payload(codes.codeFor(one.getId())));
        assertThat(receipt.admissionTickets().getFirst().code()).matches("[1-9][0-9]{7}");
    }
    @Test void sharedQrRequiresCsrfAndExplicitConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession(); session.setAttribute(Constants.SESSION_USER, staff);
        mvc.perform(get("/nhan-vien/soat-ve").param("qr", bundle()).session(session)).andExpect(status().isOk());
        mvc.perform(post("/nhan-vien/soat-ve/hoa-don/{code}/vao-phong", invoice()).param("qr", bundle()).session(session))
                .andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
        mvc.perform(post("/nhan-vien/soat-ve/hoa-don/{code}/vao-phong", invoice()).param("qr", bundle())
                        .param("checkInCsrf", security.token(session)).session(session))
                .andExpect(flash().attributeExists(Constants.MODEL_SUCCESS_MESSAGE));
        assertThat(lookup.checkBookingQr(bundle()).tickets()).allSatisfy(t -> assertThat(t.getVerdict()).isEqualTo(Verdict.CHECKED_IN));
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
    }
    @Test void usedSeatIsSkippedWhenAdmittingRemainingGroupSeats() {
        Long usedId = paid.getFirst().getId();
        lookup.checkIn(usedId);
        LocalDateTime usedAt = ticketRepository.findById(usedId).orElseThrow().getCheckedInAt();
        assertThat(lookup.checkBookingQr(bundle()).validCount()).isEqualTo(1);
        assertThat(lookup.checkInBooking(invoice(), bundle())).isEqualTo(1);
        assertThat(ticketRepository.findById(usedId).orElseThrow().getCheckedInAt()).isEqualTo(usedAt);
        assertThat(lookup.checkBookingQr(bundle()).canCheckIn()).isFalse();
    }
    @Test void wrongNumericCodeOrReceiptCannotAdmitAnotherSeatOrOrder() {
        Ticket separate = buyThirdSeat();
        assertThatThrownBy(() -> lookup.checkIn(paid.getFirst().getId(), codes.codeFor(separate.getId())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), codes.bookingPayload(separate.getBookingOrder().getReceiptCode())))
                .isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
    @Test void paidGroupWithAnUnpaidSeatIsRejectedAsAWhole() {
        jdbcTemplate.update("update tickets set status='HELD' where id=?", paid.getLast().getId());
        assertThat(lookup.checkBookingQr(bundle()).canCheckIn()).isFalse();
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
    @Test void groupRejectsSeatBelongingToAnotherCustomer() {
        jdbcTemplate.update("update tickets set user_id=? where id=?", stranger.getId(), paid.getLast().getId());
        assertThat(lookup.checkBookingQr(bundle()).tickets()).anyMatch(t -> t.getVerdict() == Verdict.INVALID);
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
    @Test void groupRejectsInactiveMovieWrongDayAndEndedShowtime() {
        movie.setActive(false); movieRepository.saveAndFlush(movie);
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        movie.setActive(true); movieRepository.saveAndFlush(movie);
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().plusDays(2), clock.now().plusDays(2).plusHours(2), show.getId());
        assertThat(lookup.checkBookingQr(bundle()).tickets()).allSatisfy(t -> assertThat(t.getVerdict()).isEqualTo(Verdict.WRONG_DAY));
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().minusHours(3), clock.now().minusHours(1), show.getId());
        assertThat(lookup.checkBookingQr(bundle()).tickets()).allSatisfy(t -> assertThat(t.getVerdict()).isEqualTo(Verdict.ENDED));
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
    @Test void cancelledSeatKeepsCodeAndCannotReappearInSharedQr() {
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().plusDays(3), clock.now().plusDays(3).plusHours(2), show.getId());
        Long cancelledId = paid.getFirst().getId(); String oldCode = codes.codeFor(cancelledId);
        refunds.cancelPaidTicket(owner.getId(), cancelledId, clock.now());
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().minusMinutes(30), clock.now().plusMinutes(90), show.getId());
        var result = lookup.checkBookingQr(bundle());
        assertThat(result.refundedTickets()).singleElement().satisfies(t -> assertThat(t.code()).isEqualTo(oldCode));
        assertThat(result.validCount()).isEqualTo(1);
        assertThat(lookup.checkInBooking(invoice(), bundle())).isEqualTo(1);
        assertThatThrownBy(() -> lookup.checkTicketCode(oldCode)).isInstanceOf(BusinessException.class);
        assertThat(codes.codeFor(cancelledId)).isEqualTo(oldCode);
    }
    @Test void allCancelledInvoiceHasNoQrAndCannotBeAdmitted() throws Exception {
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().plusDays(3), clock.now().plusDays(3).plusHours(2), show.getId());
        for (Ticket t : paid) refunds.cancelPaidTicket(owner.getId(), t.getId(), clock.now());
        assertThat(receipts.findReceipt(invoice(), owner).admissionQrSvg()).isNull();
        assertThat(lookup.checkBookingQr(bundle()).refundedTickets()).hasSize(2);
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        mvc.perform(get("/hoa-don/{code}/qr.png", invoice()).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isNotFound());
    }
    @Test void sharedQrImageUsesProtectedInvoicePayload() throws Exception {
        mvc.perform(get("/hoa-don/{code}/qr.png", invoice())).andExpect(status().isUnauthorized());
        mvc.perform(get("/hoa-don/{code}/qr.png", invoice()).sessionAttr(Constants.SESSION_USER, stranger))
                .andExpect(status().isNotFound());
        byte[] png = mvc.perform(get("/hoa-don/{code}/qr.png", invoice()).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        var image = ImageIO.read(new ByteArrayInputStream(png));
        int w = image.getWidth(), h = image.getHeight();
        String payload = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,
                image.getRGB(0,0,w,h,null,0,w))))).getText();
        assertThat(payload).isEqualTo(bundle());
    }
    @Test void concurrentGroupConfirmationAdmitsEachSeatOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> admit = () -> { start.await(); try { return lookup.checkInBooking(invoice(), bundle()); }
                catch (BusinessException error) { return 0; } };
            Future<Integer> a = pool.submit(admit), b = pool.submit(admit); start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS), b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(2,0);
        }
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNotNull());
    }
    @Test void refundedSeatsAreLinkedByOriginalInvoiceEvenWhenPaymentTimestampMatches() {
        Ticket separate = buyThirdSeat();
        jdbcTemplate.update("update showtimes set start_time=?,end_time=? where id=?",
                clock.now().plusDays(3), clock.now().plusDays(3).plusHours(2), show.getId());
        var otherRefund = refunds.cancelPaidTicket(owner.getId(), separate.getId(), clock.now());
        var ownRefund = refunds.cancelPaidTicket(owner.getId(), paid.getFirst().getId(), clock.now());
        otherRefund.setPaidAt(ownRefund.getPaidAt()); ticketRefundRepository.saveAndFlush(otherRefund);
        assertThat(receipts.findReceipt(invoice(), owner).admissionTickets()).hasSize(2)
                .noneMatch(t -> t.code().equals(codes.codeFor(separate.getId())));
        assertThat(lookup.checkBookingQr(bundle()).refundedTickets()).singleElement()
                .satisfies(t -> assertThat(t.code()).isEqualTo(codes.codeFor(paid.getFirst().getId())));
    }
    @Test void groupAdmissionRollsBackEverySeatIfSecondUpdateFails() {
        // Lỗi database sau khi ghế đầu đã UPDATE: toàn bộ giao dịch phải rollback.
        Long lastId = paid.getLast().getId();
        jdbcTemplate.execute("CREATE TRIGGER trg_test_group_admission_failure ON dbo.tickets AFTER UPDATE AS "
                + "BEGIN IF UPDATE(checked_in_at) AND EXISTS(SELECT 1 FROM inserted WHERE id=" + lastId
                + " AND checked_in_at IS NOT NULL) THROW 51000, 'Test group admission failure', 1; END");
        try {
            assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(RuntimeException.class);
            assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_test_group_admission_failure");
        }
    }
    @Test void concurrentIndividualAndGroupConfirmationConsumeOnlyRemainingSeats() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var individual = pool.submit(() -> { start.await(); try { lookup.checkIn(paid.getFirst().getId()); return 1; }
                catch (BusinessException e) { return 0; } });
            var group = pool.submit(() -> { start.await(); return lookup.checkInBooking(invoice(), bundle()); });
            start.countDown();
            assertThat(individual.get(15,TimeUnit.SECONDS) + group.get(15,TimeUnit.SECONDS)).isEqualTo(2);
        }
        assertThat(lookup.checkBookingQr(bundle()).tickets()).allSatisfy(t -> assertThat(t.getVerdict()).isEqualTo(Verdict.CHECKED_IN));
    }
    @Test void malformedUnknownAndUnpaidInvoiceQrCannotBeAdmitted() {
        for (String bad : new String[]{"", "https://wallet/pay/123", bundle()+":A3", bundle().toLowerCase(),
                "UTE-CINEMA:BOOKING:V2:UTE-20260101010101-FFFFFF"}) {
            assertThatThrownBy(() -> lookup.checkBookingQr(bad)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bad)).isInstanceOf(BusinessException.class);
        }
        jdbcTemplate.update("update booking_orders set status='DRAFT' where receipt_code=?", invoice());
        assertThatThrownBy(() -> lookup.checkBookingQr(bundle())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> lookup.checkInBooking(invoice(), bundle())).isInstanceOf(BusinessException.class);
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
    @Test void customerCannotConfirmGroupEvenWithCorrectInvoiceAndQr() throws Exception {
        mvc.perform(post("/nhan-vien/soat-ve/hoa-don/{code}/vao-phong", invoice()).param("qr", bundle())
                        .sessionAttr(Constants.SESSION_USER, owner)).andExpect(status().isForbidden());
        assertThat(ticketRepository.findAll()).allSatisfy(t -> assertThat(t.getCheckedInAt()).isNull());
    }
}
