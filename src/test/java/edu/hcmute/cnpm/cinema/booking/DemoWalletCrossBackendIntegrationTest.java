package edu.hcmute.cnpm.cinema.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.cnpm.cinema.CinemaBookingApplication;
import edu.hcmute.cnpm.cinema.config.DemoWalletCompatibility;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.DemoWalletSessions;
import edu.hcmute.cnpm.cinema.dto.payment.DemoWalletRequest;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Two independent Spring backends and browser sessions sharing only the guarded test SQL database. */
@SpringBootTest(properties={"demo-wallet.enabled=true", "demo-wallet.phone-enabled=false",
        "demo-wallet.warmup-enabled=false", "demo-wallet.public-base-url=https://wallet.group8.test"})
@AutoConfigureMockMvc
class DemoWalletCrossBackendIntegrationTest extends IntegrationTestBase {
    @Autowired BookingOrderService orders;
    @Autowired DemoWalletService wallet;
    @Autowired DemoWalletSessions sessions;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DemoWalletCompatibility compatibility;

    @Test void phoneBackendPreservesVoucherAndConcessionsAcrossRepeatedReadsAndConfirmation() throws Exception {
        User user=testDataFactory.createCustomer("cross-backend@test.local");
        Movie movie=testDataFactory.createMovie("Thanh toán hai backend"); Room room=testDataFactory.createRoom("Cinema 1",1,6);
        Showtime showtime=testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(2));
        var held = new ArrayList<Ticket>();
        for (int column : List.of(1,2)) {
            Ticket ticket=testDataFactory.newHeldTicket(showtime,testDataFactory.createSeat(room,"A",column),user);
            ticket.setPrice(new BigDecimal("135000")); held.add(ticket);
        }
        var ids=ticketRepository.saveAllAndFlush(held).stream().map(Ticket::getId).toList();
        var product=testDataFactory.createConcessionProduct("CROSS-COMBO",new BigDecimal("198000"));
        orders.saveConcessions(user.getId(),showtime.getId(),Map.of(product.getId(),1));
        testDataFactory.createVoucher("UTE10");
        orders.applyVoucher(user.getId(),showtime.getId(),"UTE10",ids);
        var issued=wallet.create(user.getId(),showtime.getId(),ids,null);
        assertThat(wallet.wallet(issued.publicId(),issued.token()).amount()).isEqualTo(441000);

        // Reproduce the legacy backend overwriting the total while the voucher remains present.
        jdbcTemplate.update("UPDATE booking_orders SET total_amount=ticket_subtotal+concession_subtotal WHERE status='DRAFT'");
        try (var phoneApp = new SpringApplicationBuilder(CinemaBookingApplication.class).profiles("test").run(
                "--server.port=0", "--demo-wallet.enabled=true", "--demo-wallet.phone-enabled=true",
                "--demo-wallet.warmup-enabled=false", "--demo-wallet.gateway-enabled=false",
                "--demo-wallet.public-base-url=http://localhost", "--spring.jpa.hibernate.ddl-auto=validate",
                "--logging.level.root=WARN")) {
            int port=((ServletWebServerApplicationContext)phoneApp).getWebServer().getPort();
            URI base=URI.create("http://127.0.0.1:"+port);
            HttpClient phone=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL))
                    .connectTimeout(Duration.ofSeconds(5)).build();
            var page=phone.send(HttpRequest.newBuilder(base.resolve("/demo-wallet/pay/"+issued.publicId())).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            var csrf=Pattern.compile("data-csrf=\"([^\"]+)\"").matcher(page.body()); assertThat(csrf.find()).isTrue();
            String nonce=csrf.group(1);
            var health=phone.send(HttpRequest.newBuilder(base.resolve("/demo-wallet/health")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(json.readTree(health.body()).path("checkoutVersion").asText()).isEqualTo(DemoWalletCompatibility.CHECKOUT_VERSION);
            for (int attempt=0;attempt<3;attempt++) {
                var response=post(phone,base,issued.publicId(),"status",nonce,new DemoWalletRequest(issued.token(),null,null));
                assertThat(response.statusCode()).isEqualTo(200);
                var state=json.readTree(response.body());
                assertThat(state.path("status").asText()).isEqualTo("PENDING"); assertThat(state.path("amount").asLong()).isEqualTo(441000);
                assertThat(jdbcTemplate.queryForObject("SELECT total_amount FROM booking_orders WHERE status='DRAFT'",BigDecimal.class)).isEqualByComparingTo("441000");
                assertThat(jdbcTemplate.queryForObject("SELECT discount_amount FROM booking_orders WHERE status='DRAFT'",BigDecimal.class)).isEqualByComparingTo("27000");
            }
            assertThat(post(phone,base,issued.publicId(),"confirm",nonce,new DemoWalletRequest(issued.token(),468000L,true)).statusCode()).isEqualTo(400);
            assertThat(ticketRepository.findAll()).allMatch(t->t.getStatus()==TicketStatus.HELD);
            for (int attempt=0;attempt<2;attempt++) {
                var response=post(phone,base,issued.publicId(),"confirm",nonce,new DemoWalletRequest(issued.token(),441000L,true));
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(json.readTree(response.body()).path("status").asText()).isEqualTo("SUCCESS");
            }
        }
        var paid=bookingOrderRepository.findAll().getFirst();
        assertThat(paid.getStatus()).isEqualTo(BookingOrderStatus.PAID);
        assertThat(paid.getTotalAmount()).isEqualByComparingTo("441000"); assertThat(paid.getDiscountAmount()).isEqualByComparingTo("27000");
        assertThat(ticketRepository.findAll()).hasSize(2).allSatisfy(ticket->{
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
            assertThat(ticket.getPrice()).isEqualByComparingTo("121500"); assertThat(ticket.getOriginalPrice()).isEqualByComparingTo("135000");
        });
        assertThat(concessionStockMovementRepository.count()).isEqualTo(1);
        assertThat(ticketPublicCodeRepository.count()).isEqualTo(2);
        MockHttpSession laptop=new MockHttpSession(); laptop.setAttribute(Constants.SESSION_USER,user); sessions.remember(laptop,issued);
        mvc.perform(get("/thanh-toan/demo/qr/"+issued.publicId()+"/status").session(laptop)).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(get("/thanh-toan/demo/qr/"+issued.publicId()+"/finish").session(laptop)).andExpect(redirectedUrl("/hoa-don/"+paid.getReceiptCode()));
    }
    private HttpResponse<String> post(HttpClient phone, URI base, String id, String action, String csrf, DemoWalletRequest body) throws Exception {
        return phone.send(HttpRequest.newBuilder(base.resolve("/demo-wallet/api/"+id+"/"+action)).timeout(Duration.ofSeconds(10))
                .header("Content-Type","application/json").header("Accept","application/json").header("X-Demo-Wallet-CSRF",csrf)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
}
