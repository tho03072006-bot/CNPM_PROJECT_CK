package edu.hcmute.cnpm.cinema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.cnpm.cinema.dto.booking.BookedTicketSnapshot;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.repository.BookingOrderRepository;
import org.springframework.stereotype.Service;
import java.util.*;

/** Lưu cùng transaction đặt vé, trước mọi thao tác trả/xóa ghế. */
@Service
public class BookingSnapshotService {
    private final ObjectMapper json;
    private final BookingOrderRepository orders;

    public BookingSnapshotService(ObjectMapper json, BookingOrderRepository orders) {
        this.json = json; this.orders = orders;
    }

    public List<BookedTicketSnapshot> read(BookingOrder order) {
        if (order.getTicketSnapshot() == null || order.getTicketSnapshot().isBlank()) {
            return order.getTickets().stream().map(this::fromTicket).toList();
        }
        try { return json.readValue(order.getTicketSnapshot(), new TypeReference<>() {}); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không đọc được bản lưu vé của đơn " + order.getReceiptCode(), exception);
        }
    }

    public void capture(BookingOrder order, List<Ticket> tickets) {
        try { order.setTicketSnapshot(json.writeValueAsString(tickets.stream()
                .sorted(Comparator.comparing(Ticket::getId)).map(this::fromTicket).toList())); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không lưu được thông tin vé của đơn " + order.getReceiptCode(), exception);
        }
    }

    public boolean belongsToSameHold(BookingOrder order, List<Ticket> tickets) {
        List<BookedTicketSnapshot> saved = read(order);
        Set<Long> savedIds = new HashSet<>(saved.stream().map(BookedTicketSnapshot::ticketId).toList());
        if (tickets.stream().anyMatch(ticket -> savedIds.contains(ticket.getId()))) return true;
        // Đơn cũ chưa có snapshot vẫn được tiếp tục nếu còn vé của đúng đơn đó.
        if (saved.isEmpty()) return false;
        return saved.stream().map(BookedTicketSnapshot::heldAt).filter(Objects::nonNull)
                .findFirst().map(heldAt -> tickets.stream().allMatch(ticket -> ticket.getHeldAt() != null
                        && java.time.Duration.between(heldAt, ticket.getHeldAt()).abs().toNanos() <= 1000))
                .orElse(false);
    }

    public void closeDraftsBeforeRelease(List<Ticket> released) {
        Map<Long, BookingOrder> affected = new LinkedHashMap<>();
        for (Ticket ticket : released) {
            BookingOrder order = ticket.getBookingOrder();
            if (order != null && order.getStatus() == BookingOrderStatus.DRAFT) affected.put(order.getId(), order);
        }
        Set<Long> ids = new HashSet<>(released.stream().map(Ticket::getId).toList());
        for (BookingOrder order : affected.values()) {
            if (order.getTickets().stream().allMatch(ticket -> ids.contains(ticket.getId()))) {
                if (order.getTicketSnapshot() == null || order.getTicketSnapshot().isBlank()) {
                    capture(order, order.getTickets());
                }
                order.setStatus(BookingOrderStatus.CANCELLED);
                orders.saveAndFlush(order);
            }
        }
    }

    private BookedTicketSnapshot fromTicket(Ticket ticket) {
        return new BookedTicketSnapshot(ticket.getId(), ticket.getSeat().getSeatRow() + ticket.getSeat().getSeatColumn(),
                ticket.getSeat().getSeatType(), ticket.getOriginalPrice(), ticket.getHeldAt());
    }
}
