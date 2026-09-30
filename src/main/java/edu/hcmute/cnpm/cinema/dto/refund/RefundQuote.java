package edu.hcmute.cnpm.cinema.dto.refund;

import edu.hcmute.cnpm.cinema.entity.Ticket;

import java.math.BigDecimal;

/** Báo giá hoàn tiền cho một vé: hoàn bao nhiêu phần trăm, bao nhiêu tiền, vì sao. */
public class RefundQuote {

    private final Ticket ticket;
    private final int refundPercent;
    private final BigDecimal refundAmount;
    private final String explanation;

    public RefundQuote(Ticket ticket, int refundPercent, BigDecimal refundAmount, String explanation) {
        this.ticket = ticket;
        this.refundPercent = refundPercent;
        this.refundAmount = refundAmount;
        this.explanation = explanation;
    }

    public Ticket getTicket() { return ticket; }
    public int getRefundPercent() { return refundPercent; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getExplanation() { return explanation; }
}
