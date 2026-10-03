package edu.hcmute.cnpm.cinema.config;
import edu.hcmute.cnpm.cinema.service.TicketCodeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** Cấp mã cho vé cũ khi chạy Spring Boot, giữ nguyên mọi dữ liệu vé. */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.ticket-code.backfill.enabled", havingValue = "true", matchIfMissing = true)
public class TicketCodeBackfill implements ApplicationRunner {
    private final TicketCodeService codes;
    private final JdbcTemplate jdbc;
    public TicketCodeBackfill(TicketCodeService codes, JdbcTemplate jdbc) { this.codes = codes; this.jdbc = jdbc; }
    public void run(ApplicationArguments args) {
        jdbc.update("""
                UPDATE r SET booking_order_id = matches.order_id
                FROM ticket_refunds r
                CROSS APPLY (SELECT MIN(o.id) order_id, COUNT(*) amount FROM booking_orders o
                 WHERE o.user_id = r.user_id AND o.showtime_id = r.showtime_id AND o.status = 'PAID'
                 AND o.paid_at = r.paid_at AND (o.payment_ref = r.payment_ref OR (o.payment_ref IS NULL AND r.payment_ref IS NULL))) matches
                WHERE r.booking_order_id IS NULL AND matches.amount = 1
                """);
        var ids = jdbc.queryForList("""
                select t.id from tickets t where t.status = 'PAID'
                  and not exists (select 1 from ticket_public_codes c where c.ticket_id = t.id)
                union select r.original_ticket_id from ticket_refunds r
                  where not exists (select 1 from ticket_public_codes c where c.ticket_id = r.original_ticket_id)
                """, Long.class);
        for (int offset = 0; offset < ids.size(); offset += 100)
            codes.codesFor(ids.subList(offset, Math.min(offset + 100, ids.size())));
    }
}
