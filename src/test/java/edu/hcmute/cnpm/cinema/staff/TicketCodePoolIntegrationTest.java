package edu.hcmute.cnpm.cinema.staff;
import edu.hcmute.cnpm.cinema.service.TicketCodeService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Cấp mã phải dùng connection của transaction ngoài, không đòi thêm connection khi pool đã đầy. */
@SpringBootTest(properties={"spring.datasource.hikari.maximum-pool-size=2","spring.datasource.hikari.connection-timeout=2000"})
class TicketCodePoolIntegrationTest extends IntegrationTestBase {
    @Autowired TicketCodeService codes;
    @Autowired PlatformTransactionManager transactions;
    @Test void codeAllocationWorksWhenAllPoolConnectionsBelongToOuterTransactions() throws Exception {
        CountDownLatch connected=new CountDownLatch(2), start=new CountDownLatch(1);
        try(ExecutorService pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->allocateInOuterTransaction(101L,connected,start));
            var b=pool.submit(()->allocateInOuterTransaction(102L,connected,start));
            assertThat(connected.await(10,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(a.get(10,TimeUnit.SECONDS)).matches("[1-9][0-9]{7}");
            assertThat(b.get(10,TimeUnit.SECONDS)).matches("[1-9][0-9]{7}");
        } finally { start.countDown(); }
        assertThat(ticketPublicCodeRepository.findAll()).hasSize(2)
                .extracting(c->c.getPublicCode()).doesNotHaveDuplicates();
    }
    private String allocateInOuterTransaction(Long id,CountDownLatch connected,CountDownLatch start){
        return new TransactionTemplate(transactions).execute(status->{
            jdbcTemplate.queryForObject("select 1",Integer.class);
            connected.countDown();
            try{
                if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Outer transactions did not start");
            }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            return codes.codeFor(id);
        });
    }
}
