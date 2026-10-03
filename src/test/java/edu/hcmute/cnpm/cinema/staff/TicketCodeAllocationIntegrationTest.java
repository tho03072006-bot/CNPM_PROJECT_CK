package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketCodeAllocationIntegrationTest extends IntegrationTestBase {
    @Autowired TicketCodeService codes;
    @MockitoBean TicketCodeGenerator generator;
    @Test void repeatedReadsKeepExactlyTheSameCode() {
        when(generator.nextCode()).thenReturn("12345678");
        assertThat(codes.codeFor(101L)).isEqualTo("12345678");
        assertThat(codes.codeFor(101L)).isEqualTo("12345678");
        assertThat(codes.resolve("12345678")).isEqualTo(101L);
        verify(generator, times(1)).nextCode();
    }
    @Test void duplicateCandidateIsRetriedInsteadOfOverwritingCode() {
        when(generator.nextCode()).thenReturn("12345678", "12345678", "87654321");
        assertThat(codes.codeFor(101L)).isEqualTo("12345678");
        assertThat(codes.codeFor(102L)).isEqualTo("87654321");
        assertThat(codes.resolve("12345678")).isEqualTo(101L);
        assertThat(ticketPublicCodeRepository.count()).isEqualTo(2);
    }
    @Test void exhaustingRetriesFailsWithoutReassigningExistingCode() {
        when(generator.nextCode()).thenReturn("12345678");
        codes.codeFor(101L);
        assertThatThrownBy(() -> codes.codeFor(102L)).isInstanceOf(BusinessException.class);
        assertThat(ticketPublicCodeRepository.count()).isEqualTo(1);
        assertThat(codes.resolve("12345678")).isEqualTo(101L);
    }
    @Test void invalidIdsAreRejectedBeforeAllocation() {
        for (Long id : Arrays.asList(null, 0L, -1L))
            assertThatThrownBy(() -> codes.codeFor(id)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> codes.codesFor(Arrays.asList(1L,null))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(generator);
    }
    @Test void concurrentMachinesRequestingSameIdReceiveSameCode() throws Exception {
        when(generator.nextCode()).thenReturn("12345678", "87654321");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<String> allocate = () -> { start.await(); return codes.codeFor(101L); };
            var a = pool.submit(allocate); var b = pool.submit(allocate); start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS), b.get(15,TimeUnit.SECONDS)))
                    .containsExactly("12345678","12345678");
        }
        assertThat(ticketPublicCodeRepository.count()).isEqualTo(1);
    }
    @Test void databaseEnforcesFormatAndUniquenessEvenForDirectInsert() {
        when(generator.nextCode()).thenReturn("12345678"); codes.codeFor(101L);
        for (String bad : List.of("12345678","12","01234567","1234abcd"))
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into ticket_public_codes(ticket_id,public_code,created_at) values(102,?,sysdatetime())", bad))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(ticketPublicCodeRepository.count()).isEqualTo(1);
    }
}
