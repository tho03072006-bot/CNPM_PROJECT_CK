package edu.hcmute.cnpm.cinema.controller;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
public class TicketQrController {
    private final TicketRepository tickets;
    private final TicketCodeService codes;
    private final QrCodeService qr;
    private final ReceiptService receipts;
    public TicketQrController(TicketRepository tickets, TicketCodeService codes, QrCodeService qr, ReceiptService receipts) {
        this.receipts = receipts;
        this.tickets = tickets; this.codes = codes; this.qr = qr;
    }
    @GetMapping(value = "/ve/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Transactional
    public ResponseEntity<byte[]> image(@PathVariable Long id, HttpSession session) {
        User viewer = SessionUsers.current(session);
        if (viewer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Ticket ticket = id <= 0 ? null : tickets.findById(id).orElse(null);
        if (ticket == null || ticket.getStatus() != TicketStatus.PAID
                || (!ticket.getUser().getId().equals(viewer.getId())
                    && viewer.getRole() != Role.STAFF && viewer.getRole() != Role.ADMIN))
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG).body(qr.toPng(codes.payload(codes.codeFor(id))));
    }
    @GetMapping(value = "/hoa-don/{receiptCode}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> bookingImage(@PathVariable String receiptCode, HttpSession session) {
        User viewer = SessionUsers.current(session);
        if (viewer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            var receipt = receipts.findReceipt(receiptCode, viewer);
            if (receipt.admissionPayload() == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .contentType(MediaType.IMAGE_PNG).body(qr.toPng(receipt.admissionPayload()));
        } catch (edu.hcmute.cnpm.cinema.exception.BusinessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
