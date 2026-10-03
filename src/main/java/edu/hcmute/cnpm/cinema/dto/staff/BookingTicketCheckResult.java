package edu.hcmute.cnpm.cinema.dto.staff;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptTicket;
import java.util.List;
public record BookingTicketCheckResult(BookingOrder order, List<TicketCheckResult> tickets,
                                      List<ReceiptTicket> refundedTickets) {
    public long validCount() { return tickets.stream().filter(TicketCheckResult::isValid).count(); }
    public boolean canCheckIn() { return validCount() > 0 && tickets.stream().allMatch(t ->
            t.isValid() || t.getVerdict() == TicketCheckResult.Verdict.CHECKED_IN); }
}
