package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.DemoWalletSessions;
import edu.hcmute.cnpm.cinema.dto.payment.DemoWalletRequest;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SQL Server thật, hai thiết bị dùng hai session độc lập. © Nhóm 8. */
@SpringBootTest(properties={"demo-wallet.enabled=true","demo-wallet.public-base-url=https://demo.group8.test/demo-wallet/"})
@AutoConfigureMockMvc
class DemoWalletIntegrationTest extends IntegrationTestBase {
    @Autowired private DemoWalletService wallet;
    @Autowired private DemoWalletSessions sessions;
    @Autowired private SeatHoldService holds;
    @Autowired private PaymentService payment;
    @Autowired private BookingOrderService orders;
    @Autowired private TicketRefundService refunds;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private BookingClock clock;
    private record Fixture(User user,Showtime showtime,List<Seat> seats,List<Ticket> tickets) {
        List<Long> ids(){return tickets.stream().map(Ticket::getId).toList();}
    }
    private Fixture fixture(){
        User user=testDataFactory.createCustomer("wallet@test.local");
        Movie movie=testDataFactory.createMovie("Phim thử ví");Room room=testDataFactory.createRoom("Cinema 1",1,6);
        List<Seat> seats=new ArrayList<>();
        for(int i=1;i<=6;i++)seats.add(testDataFactory.createSeat(room,"A",i));
        Showtime showtime=testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(2));
        List<Ticket> tickets=ticketRepository.saveAllAndFlush(List.of(
            testDataFactory.newHeldTicket(showtime,seats.get(0),user),testDataFactory.newHeldTicket(showtime,seats.get(1),user)));
        return new Fixture(user,showtime,seats,tickets);
    }
    private DemoWalletService.Issued issue(Fixture data){
        return wallet.create(data.user().getId(),data.showtime().getId(),data.ids(),null);
    }
    private DemoWalletRequest confirm(DemoWalletService.Issued issued,long amount){return new DemoWalletRequest(issued.token(),amount,true);}
    private void assertHeld(Fixture data){assertThat(ticketRepository.findAll()).hasSize(2).allMatch(t->t.getStatus()==TicketStatus.HELD);}

    @Test @DisplayName("Laptop tạo QR, điện thoại không đăng nhập xác nhận và laptop nhận vé")
    void shouldCompletePayment_whenPhoneUsesIndependentSession() throws Exception{
        Fixture data=fixture();MockHttpSession laptop=new MockHttpSession();laptop.setAttribute(Constants.SESSION_USER,data.user());
        String laptopCsrf=sessions.csrf(laptop);
        var created=mvc.perform(post("/thanh-toan/{id}/demo-wallet",data.showtime().getId())
                .session(laptop).param("ticketIds",data.ids().stream().map(String::valueOf).toArray(String[]::new))
                .param("walletCsrf",laptopCsrf)).andExpect(status().is3xxRedirection()).andReturn();
        String qrUrl=created.getResponse().getRedirectedUrl();
        String publicId=qrUrl.substring(qrUrl.lastIndexOf('/')+1);
        var issued=sessions.find(laptop,publicId);
        mvc.perform(get(qrUrl).session(laptop)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://demo.group8.test/demo-wallet/pay/")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/demo-wallet/demo-wallet/"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhóm 8")));
        MockHttpSession phone=new MockHttpSession();
        mvc.perform(get("/demo-wallet/pay/{id}",publicId).session(phone)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không thuộc hệ thống MoMo chính thức")));
        String phoneCsrf=sessions.csrf(phone);
        mvc.perform(post("/demo-wallet/api/{id}/status",publicId).session(phone)
                .header("X-Demo-Wallet-CSRF",phoneCsrf).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+issued.token()+"\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(post("/demo-wallet/api/{id}/confirm",publicId).session(phone)
                .header("X-Demo-Wallet-CSRF",phoneCsrf).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+issued.token()+"\",\"expectedAmount\":150000,\"confirmed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(get(qrUrl+"/status").session(laptop).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.userId").doesNotExist());
        String invoice = "/hoa-don/" + bookingOrderRepository.findAll().getFirst().getReceiptCode();
        mvc.perform(get(qrUrl+"/finish").session(laptop)).andExpect(redirectedUrl(invoice));
        mvc.perform(get(invoice).session(laptop)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mã vé (8 số)")));
        assertThat(ticketRepository.findAll()).allMatch(t->t.getStatus()==TicketStatus.PAID&&t.getPaymentMethod()==PaymentMethod.MOMO_DEMO);
        verifyNoInteractions(momoApiClient);
    }
    @Test @DisplayName("Áp dụng voucher vô hiệu QR giá cũ; QR mới thu đúng tiền vé sau giảm")
    void shouldInvalidateOldQrAndPayDiscountedAmount_whenVoucherApplied() {
        Fixture data = fixture();
        testDataFactory.createVoucher("UTE10");
        var old = issue(data);
        orders.applyVoucher(data.user().getId(), data.showtime().getId(), "UTE10", data.ids());
        assertThat(wallet.confirm(old.publicId(), confirm(old, 150000)).status()).isEqualTo("INVALIDATED");
        assertHeld(data);
        var current = issue(data);
        assertThat(wallet.wallet(current.publicId(), current.token()).amount()).isEqualTo(135000);
        assertThat(wallet.confirm(current.publicId(), confirm(current, 135000)).status()).isEqualTo("SUCCESS");
        assertThat(ticketRepository.findAll()).allSatisfy(ticket -> {
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
            assertThat(ticket.getPrice()).isEqualByComparingTo("67500");
            assertThat(ticket.getOriginalPrice()).isEqualByComparingTo("75000");
        });
        var paidOrder = bookingOrderRepository.findAll().getFirst();
        assertThat(paidOrder.getTotalAmount()).isEqualByComparingTo("135000");
        assertThat(paidOrder.getDiscountAmount()).isEqualByComparingTo("15000");
        verifyNoInteractions(momoApiClient);
    }

    @Test @DisplayName("Voucher ngừng áp dụng làm QR đã tạo mất hiệu lực trước khi thu tiền")
    void shouldInvalidateDiscountedQr_whenVoucherDisabled() {
        Fixture data = fixture();
        testDataFactory.createVoucher("UTE10");
        orders.applyVoucher(data.user().getId(), data.showtime().getId(), "UTE10", data.ids());
        var old = issue(data);
        jdbcTemplate.update("UPDATE vouchers SET active = 0 WHERE code = 'UTE10'");
        assertThat(wallet.confirm(old.publicId(), confirm(old, 135000)).status()).isEqualTo("INVALIDATED");
        assertHeld(data);
        var current = issue(data);
        assertThat(wallet.wallet(current.publicId(), current.token()).amount()).isEqualTo(150000);
        assertThat(wallet.confirm(current.publicId(), confirm(current, 150000)).status()).isEqualTo("SUCCESS");
    }

    @Test @DisplayName("Tạo lại cùng lượt giữ trong cùng phiên giữ nguyên QR và hạn")
    void shouldReuseQr_whenSameCheckoutIsRetried(){
        Fixture data=fixture();var issued=issue(data);
        var retry=wallet.create(data.user().getId(),data.showtime().getId(),data.ids(),issued);
        assertThat(retry).isEqualTo(issued);assertThat(demoPaymentRepository.count()).isEqualTo(1);
        assertThat(wallet.wallet(issued.publicId(),issued.token()).expiresAtMillis())
                .isEqualTo(clock.epochMillis(data.tickets().getFirst().getHeldAt().plusMinutes(5)));
    }
    @Test @DisplayName("Tạo QR mới làm QR cũ mất hiệu lực")
    void shouldInvalidateOldQr_whenNewLinkIsCreated(){
        Fixture data=fixture();var old=issue(data);var current=issue(data);
        assertThat(current.publicId()).isNotEqualTo(old.publicId());
        assertThat(wallet.confirm(old.publicId(),confirm(old,150000)).status()).isEqualTo("INVALIDATED");
        assertHeld(data);assertThat(wallet.wallet(current.publicId(),current.token()).status()).isEqualTo("PENDING");
    }
    @ParameterizedTest @ValueSource(longs={-1,0,149999,150001})
    @DisplayName("Không thanh toán nếu số tiền khác số tiền máy chủ")
    void shouldRejectPayment_whenAmountIsInvalid(long amount){
        Fixture data=fixture();var issued=issue(data);
        assertThatThrownBy(()->wallet.confirm(issued.publicId(),confirm(issued,amount))).isInstanceOf(BusinessException.class);
        assertHeld(data);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"wrong","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("Khoá thiếu hoặc sai không thể đọc hay xác nhận giao dịch")
    void shouldRejectWalletAccess_whenTokenIsInvalid(String token){
        Fixture data=fixture();var issued=issue(data);
        assertThatThrownBy(()->wallet.wallet(issued.publicId(),token)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->wallet.confirm(issued.publicId(),new DemoWalletRequest(token,150000L,true))).isInstanceOf(BusinessException.class);
        assertHeld(data);
    }
    @Test @DisplayName("Thiếu xác nhận giả lập không xuất vé")
    void shouldRejectPayment_whenConsentIsMissing(){
        Fixture data=fixture();var issued=issue(data);
        assertThatThrownBy(()->wallet.confirm(issued.publicId(),new DemoWalletRequest(issued.token(),150000L,false)))
                .isInstanceOf(BusinessException.class);assertHeld(data);
    }
    @Test @DisplayName("Hết hạn tại đúng mốc năm phút")
    void shouldExpirePayment_whenExactDeadlineIsReached(){
        Fixture data=fixture();var issued=issue(data);
        LocalDateTime deadline=demoPaymentRepository.findByPublicId(issued.publicId()).orElseThrow().getExpiresAt();
        doReturn(deadline).when(clock).now();
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("EXPIRED");assertHeld(data);
    }
    @Test @DisplayName("QR cũ không trả cho lượt giữ mới có cùng số tiền")
    void shouldInvalidatePayment_whenHoldIsReplaced(){
        Fixture data=fixture();var issued=issue(data);
        holds.cancelHold(data.user().getId(),data.showtime().getId(),data.ids());
        ticketRepository.saveAllAndFlush(List.of(testDataFactory.newHeldTicket(data.showtime(),data.seats().get(4),data.user()),
                testDataFactory.newHeldTicket(data.showtime(),data.seats().get(5),data.user())));
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("INVALIDATED");
        assertThat(ticketRepository.findAll()).allMatch(t->t.getStatus()==TicketStatus.HELD);
    }
    @Test @DisplayName("Đổi bắp nước sau tạo QR bắt buộc tạo giao dịch mới")
    void shouldInvalidatePayment_whenConcessionTotalChanges(){
        Fixture data=fixture();var issued=issue(data);
        var product=testDataFactory.createConcessionProduct("POP",new BigDecimal("25000"));
        orders.saveConcessions(data.user().getId(),data.showtime().getId(),Map.of(product.getId(),1));
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("INVALIDATED");assertHeld(data);
    }
    @Test @DisplayName("Ví xác nhận cả tiền vé và bắp nước")
    void shouldPayFullOrder_whenConcessionsAreIncluded(){
        Fixture data=fixture();var product=testDataFactory.createConcessionProduct("POP",new BigDecimal("25000"));
        orders.saveConcessions(data.user().getId(),data.showtime().getId(),Map.of(product.getId(),2));
        var issued=issue(data);assertThat(wallet.wallet(issued.publicId(),issued.token()).amount()).isEqualTo(200000);
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,200000)).status()).isEqualTo("SUCCESS");
        assertThat(bookingOrderRepository.findAll()).singleElement().satisfies(order->{
            assertThat(order.getTotalAmount()).isEqualByComparingTo("200000");assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.MOMO_DEMO);
        });
    }
    @Test @DisplayName("Huỷ giao dịch không huỷ ghế và không gia hạn giữ")
    void shouldKeepOriginalHold_whenWalletTransactionIsCancelled(){
        Fixture data=fixture();var issued=issue(data);
        LocalDateTime originalHeldAt=ticketRepository.findById(data.ids().getFirst()).orElseThrow().getHeldAt();
        assertThat(wallet.cancel(issued.publicId(),issued.token()).status()).isEqualTo("CANCELLED");
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("CANCELLED");
        assertHeld(data);assertThat(ticketRepository.findAll().getFirst().getHeldAt()).isEqualTo(originalHeldAt);
    }
    @Test @DisplayName("Xác nhận trùng không tạo vé hoặc hoá đơn trùng")
    void shouldReturnSameResult_whenConfirmationIsRepeated(){
        Fixture data=fixture();var issued=issue(data);
        wallet.confirm(issued.publicId(),confirm(issued,150000));wallet.confirm(issued.publicId(),confirm(issued,150000));
        assertThat(ticketRepository.count()).isEqualTo(2);assertThat(bookingOrderRepository.count()).isEqualTo(1);
        assertThat(demoPaymentRepository.findByPublicId(issued.publicId()).orElseThrow().getStatus()).isEqualTo(DemoPaymentStatus.SUCCESS);
    }
    @Test @DisplayName("Hai điện thoại xác nhận cùng lúc chỉ xuất một lượt vé")
    void shouldConfirmOnce_whenTwoPhonesRace() throws Exception{
        Fixture data=fixture();var issued=issue(data);ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try{
            Callable<String> action=()->{start.await();return wallet.confirm(issued.publicId(),confirm(issued,150000)).status();};
            Future<String> first=executor.submit(action),second=executor.submit(action);start.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo("SUCCESS");assertThat(second.get(15,TimeUnit.SECONDS)).isEqualTo("SUCCESS");
            assertThat(ticketRepository.count()).isEqualTo(2);assertThat(bookingOrderRepository.count()).isEqualTo(1);
        }finally{executor.shutdownNow();}
    }
    @Test @DisplayName("Vé trả tại quầy không bị QR giả lập cũ xác nhận lần nữa")
    void shouldInvalidateQr_whenCounterPaymentAlreadyCompleted(){
        Fixture data=fixture();var issued=issue(data);
        payment.confirmCounterPayment(data.user().getId(),data.showtime().getId(),data.ids());
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("INVALIDATED");
        assertThat(ticketRepository.findAll()).allMatch(t->t.getPaymentMethod()==PaymentMethod.COUNTER);
    }
    @Test @DisplayName("Khách khác không thể xem QR, trạng thái hoặc kết quả")
    void shouldHideMerchantPayment_whenViewerIsNotOwner() throws Exception{
        Fixture data=fixture();var issued=issue(data);User other=testDataFactory.createCustomer("other@test.local");
        MockHttpSession session=new MockHttpSession();session.setAttribute(Constants.SESSION_USER,other);
        mvc.perform(get("/thanh-toan/demo/qr/{id}",issued.publicId()).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        mvc.perform(get("/thanh-toan/demo/qr/{id}/status",issued.publicId()).session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());assertHeld(data);
    }
    @Test @DisplayName("Không xác nhận khi thiếu CSRF hoặc dùng CSRF của thiết bị khác")
    void shouldRejectConfirmation_whenBrowserNonceDoesNotMatch() throws Exception{
        Fixture data=fixture();var issued=issue(data);MockHttpSession phone=new MockHttpSession();sessions.csrf(phone);
        mvc.perform(post("/demo-wallet/api/{id}/confirm",issued.publicId()).session(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+issued.token()+"\",\"expectedAmount\":150000,\"confirmed\":true}"))
                .andExpect(status().isBadRequest());assertHeld(data);
    }
    @ParameterizedTest @ValueSource(strings={
        "\"expectedAmount\":\"150000\",\"confirmed\":true",
        "\"expectedAmount\":150000.5,\"confirmed\":true",
        "\"expectedAmount\":true,\"confirmed\":true",
        "\"expectedAmount\":150000,\"confirmed\":\"true\"",
        "\"expectedAmount\":150000,\"confirmed\":1"})
    @DisplayName("JSON tiền/xác nhận sai kiểu không bị tự ép kiểu")
    void shouldRejectMalformedJson_whenAmountOrConsentTypeIsWrong(String fields) throws Exception{
        Fixture data=fixture();var issued=issue(data);MockHttpSession phone=new MockHttpSession();
        mvc.perform(post("/demo-wallet/api/{id}/confirm",issued.publicId()).session(phone)
                .header("X-Demo-Wallet-CSRF",sessions.csrf(phone)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+issued.token()+"\","+fields+"}")).andExpect(status().isBadRequest());assertHeld(data);
    }
    @Test @DisplayName("Hoàn vé mô phỏng không gọi API hoàn tiền thật")
    void shouldRefundLocally_whenTicketWasPaidBySimulatedWallet(){
        Fixture data=fixture();var issued=issue(data);wallet.confirm(issued.publicId(),confirm(issued,150000));
        var refund=refunds.cancelPaidTicket(data.user().getId(),data.ids().getFirst(),clock.now());
        assertThat(refund.getPaymentMethod()).isEqualTo(PaymentMethod.MOMO_DEMO);
        verifyNoInteractions(momoApiClient);
    }
    @Test @DisplayName("Mã giao dịch không tồn tại không gây lỗi 500")
    void shouldReturnBusinessError_whenPublicIdDoesNotExist(){
        assertThatThrownBy(()->wallet.wallet("bad-id","wrong")).isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("Phim ngừng hoạt động không tạo được giao dịch và không xuất vé")
    void shouldRejectStoppedMovie(){
        Fixture data=fixture();var issued=issue(data);
        Movie movie=movieRepository.findById(data.showtime().getMovie().getId()).orElseThrow();
        movie.setActive(false);movieRepository.saveAndFlush(movie);
        assertThat(wallet.confirm(issued.publicId(),confirm(issued,150000)).status()).isEqualTo("INVALIDATED");
        assertThatThrownBy(()->issue(data)).isInstanceOf(BusinessException.class);assertHeld(data);
    }
    @Test @DisplayName("Tạo QR không chấp nhận danh sách vé thiếu hoặc của lượt khác")
    void shouldRejectWrongHoldIdentity(){
        Fixture data=fixture();
        assertThatThrownBy(()->wallet.create(data.user().getId(),data.showtime().getId(),List.of(data.ids().getFirst()),null))
                .isInstanceOf(BusinessException.class);
        assertThat(demoPaymentRepository.count()).isZero();assertHeld(data);
    }
    @Test @DisplayName("CSRF lấy từ laptop không dùng cho điện thoại")
    void shouldRejectNonceFromOtherDevice() throws Exception{
        Fixture data=fixture();var issued=issue(data);
        MockHttpSession laptop=new MockHttpSession(),phone=new MockHttpSession();sessions.csrf(phone);
        mvc.perform(post("/demo-wallet/api/{id}/confirm",issued.publicId()).session(phone)
                .header("X-Demo-Wallet-CSRF",sessions.csrf(laptop)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+issued.token()+"\",\"expectedAmount\":150000,\"confirmed\":true}"))
                .andExpect(status().isBadRequest());assertHeld(data);
    }
}
