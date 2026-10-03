package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptLine;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptView;
import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Dựng hóa đơn điện tử theo bố cục hóa đơn bán hàng thật: thông tin đơn vị bán, số hóa đơn,
 * bảng hàng hóa có STT và đơn vị tính, số tiền viết bằng chữ.
 *
 * Thông tin rạp đọc từ cấu hình app.cinema.* (có giá trị mặc định), muốn đổi thì thêm
 * vào application.properties, không phải sửa code.
 */
@Service
public class ReceiptService {

    private final BookingOrderService bookingOrderService;
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerEmail;
    private final BookingTicketDataService ticketData;

    public ReceiptService(BookingOrderService bookingOrderService,
                          @Value("${app.cinema.name:Rạp chiếu phim UTE Cinema}") String sellerName,
                          @Value("${app.cinema.address:Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh}")
                          String sellerAddress,
                          @Value("${app.cinema.email:hotro@utecinema.local}") String sellerEmail,
                          BookingTicketDataService ticketData) {
        this.bookingOrderService = bookingOrderService;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerEmail = sellerEmail;
        this.ticketData = ticketData;
    }

    @Transactional(readOnly = true)
    public ReceiptView findReceipt(String receiptCode, User viewer) {
        BookingOrder order = bookingOrderService.findReceiptForUser(receiptCode, viewer);

        BookingTicketDataService.Data data = ticketData.describe(order);
        List<ReceiptLine> lines = new ArrayList<>(data.lines());
        for (BookingOrderItem item : order.getItems()) {
            lines.add(new ReceiptLine(lines.size() + 1, item.getProductName(), "Nhận tại quầy bắp nước", "Phần",
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), false));
        }

        return new ReceiptView(order, String.format("%07d", order.getId()), lines, data.seats(),
                paymentMethodLabel(order.getPaymentMethod()),
                VietnameseMoneyWords.of(order.getTotalAmount()), sellerName, sellerAddress, sellerEmail, data.tickets(), data.refundedAmount());
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        if (method == null) {
            return "Tiền mặt tại quầy";
        }
        return switch (method) {
            case COUNTER -> "Tiền mặt tại quầy";
            case MOMO -> "Ví điện tử MoMo";
            case MOMO_DEMO -> method.getDisplayName();
        };
    }

}
