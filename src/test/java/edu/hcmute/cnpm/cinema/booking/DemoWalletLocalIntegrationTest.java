package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.DemoWalletSessions;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/** Local tạo QR HTTPS, không có API xác nhận thanh toán. Chỉ dùng database _test. */
@SpringBootTest(properties={"demo-wallet.enabled=true","demo-wallet.phone-enabled=false",
        "demo-wallet.require-public-url=true","demo-wallet.public-base-url=https://wallet.group8.test/demo-wallet/"})
@AutoConfigureMockMvc
class DemoWalletLocalIntegrationTest extends IntegrationTestBase {
    private static final String ID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    @Autowired MockMvc mvc;
    @Autowired DemoWalletSessions sessions;
    @Test void localWalletHomeRedirectsToOnline() throws Exception {
        mvc.perform(get("/demo-wallet")).andExpect(status().isFound())
                .andExpect(header().string("Location","https://wallet.group8.test/demo-wallet"));
    }
    @Test void localTransactionRedirectKeepsIdAndNeverForwardsQuery() throws Exception {
        mvc.perform(get("/demo-wallet/pay/"+ID).param("next","https://evil.test"))
                .andExpect(status().isFound()).andExpect(header().string("Location","https://wallet.group8.test/demo-wallet/pay/"+ID));
    }
    @Test void localHasNoWalletPaymentApisOrHealth() throws Exception {
        for (String action:List.of("status","confirm","cancel"))
            mvc.perform(post("/demo-wallet/api/"+ID+"/"+action).contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mvc.perform(get("/demo-wallet/health").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }
    @Test void localStillCreatesQrDirectlyToOnlineTransaction() throws Exception {
        User user=testDataFactory.createCustomer("local-wallet@test.local");
        Movie movie=testDataFactory.createMovie("Phim QR online");Room room=testDataFactory.createRoom("Cinema 1",1,6);
        Seat seat1=testDataFactory.createSeat(room,"A",1),seat2=testDataFactory.createSeat(room,"A",2);
        Showtime showtime=testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(2));
        List<Ticket> tickets=ticketRepository.saveAllAndFlush(List.of(testDataFactory.newHeldTicket(showtime,seat1,user),
                testDataFactory.newHeldTicket(showtime,seat2,user)));
        MockHttpSession session=new MockHttpSession();session.setAttribute(Constants.SESSION_USER,user);
        var result=mvc.perform(post("/thanh-toan/"+showtime.getId()+"/demo-wallet").session(session)
                .param("walletCsrf",sessions.csrf(session))
                .param("ticketIds",tickets.stream().map(t->t.getId().toString()).toArray(String[]::new)))
                .andExpect(status().is3xxRedirection()).andReturn();
        mvc.perform(get(result.getResponse().getRedirectedUrl()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("https://wallet.group8.test/demo-wallet/pay/")))
                .andExpect(content().string(not(containsString("/demo-wallet/demo-wallet/"))))
                .andExpect(content().string(not(containsString("http://localhost:8082/demo-wallet/pay/"))));
    }
}
