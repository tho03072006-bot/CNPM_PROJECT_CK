package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.TicketPublicCode;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.TicketPublicCodeRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.sql.Types;
import java.util.*;

/** Mã ngẫu nhiên 8 số, cố định và không tái sử dụng, tách khỏi ID nội bộ. */
@Service
public class TicketCodeService {
    public static final String QR_PREFIX = "UTE-CINEMA:TICKET:V2:";
    public static final String BOOKING_QR_PREFIX = "UTE-CINEMA:BOOKING:V2:";
    private final TicketPublicCodeRepository repository;
    private final JdbcTemplate jdbc;
    private final TicketCodeGenerator generator;
    private final BookingClock clock;
    public TicketCodeService(TicketPublicCodeRepository repository, JdbcTemplate jdbc,
                             TicketCodeGenerator generator, BookingClock clock) {
        this.repository = repository; this.jdbc = jdbc; this.generator = generator; this.clock = clock;
    }

    public String parse(String value) {
        String code = value == null ? "" : value.trim();
        if (code.startsWith(QR_PREFIX)) code = code.substring(QR_PREFIX.length());
        else if (code.startsWith("#")) code = code.substring(1);
        if (!code.matches("[1-9][0-9]{7}"))
            throw new BusinessException("Mã vé chỉ gồm chữ số và phải có đúng 8 số, ví dụ 12345678. Mã vé cũ đã được thay thế.");
        return code;
    }
    public String payload(String publicCode) { return QR_PREFIX + parse(publicCode); }
    public String bookingPayload(String receiptCode) {
        if (receiptCode == null || !receiptCode.matches("UTE-[0-9]{14}-[A-F0-9]{6}"))
            throw new BusinessException("Mã hóa đơn trong QR không hợp lệ.");
        return BOOKING_QR_PREFIX + receiptCode;
    }
    public String parseBooking(String payload) {
        if (payload == null || !payload.startsWith(BOOKING_QR_PREFIX))
            throw new BusinessException("QR không phải vé nhóm UTE Cinema.");
        String code = payload.substring(BOOKING_QR_PREFIX.length());
        bookingPayload(code);
        return code;
    }
    @Transactional(readOnly = true)
    public Long resolve(String value) {
        String publicCode = parse(value);
        return repository.findByPublicCode(publicCode).map(TicketPublicCode::getTicketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy vé có mã " + publicCode + "."));
    }

    @Transactional
    public String codeFor(Long ticketId) {
        if (ticketId == null || ticketId <= 0) throw new BusinessException("Định danh vé không hợp lệ.");
        return allocate(List.of(ticketId)).get(ticketId);
    }

    @Transactional
    public Map<Long, String> codesFor(Collection<Long> ticketIds) { return allocate(ticketIds); }

    private Map<Long, String> allocate(Collection<Long> ticketIds) {
        if (ticketIds == null || ticketIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException("Định danh vé không hợp lệ.");
        Map<Long, String> result = new LinkedHashMap<>();
        for (TicketPublicCode row : repository.findAllById(ticketIds)) result.put(row.getTicketId(), row.getPublicCode());
        if (result.size() == new HashSet<>(ticketIds).size()) return result;
        // Mọi máy dùng cùng khóa database. Ràng buộc UNIQUE là chốt chặn cuối.
        Integer lockResult = jdbc.execute((ConnectionCallback<Integer>) connection -> {
            try (var call = connection.prepareCall("{? = call sys.sp_getapplock(?,?,?,?,?)}")) {
                call.registerOutParameter(1, Types.INTEGER);
                call.setString(2, "UTE_CINEMA_TICKET_PUBLIC_CODES_V2");
                call.setString(3, "Exclusive"); call.setString(4, "Transaction");
                call.setInt(5, 10000); call.setString(6, "public"); call.execute();
                return call.getInt(1);
            }
        });
        if (lockResult == null || lockResult < 0)
            throw new BusinessException("Hệ thống đang cấp mã vé. Vui lòng thử lại trong giây lát.");
        for (Long id : new LinkedHashSet<>(ticketIds)) {
            // Đọc lại sau khi chờ khóa: máy khác có thể vừa cấp mã cho cùng vé.
            var existing = repository.findById(id);
            if (existing.isPresent()) { result.put(id, existing.get().getPublicCode()); continue; }
            String generated = null;
            for (int attempt = 0; attempt < 64; attempt++) {
                String candidate = generator.nextCode();
                if (!candidate.matches("[1-9][0-9]{7}")) throw new IllegalStateException("Bộ sinh mã vé không hợp lệ.");
                if (!repository.existsByPublicCode(candidate)) { generated = candidate; break; }
            }
            if (generated == null) throw new BusinessException("Chưa cấp được mã vé duy nhất. Vui lòng thử lại.");
            repository.saveAndFlush(new TicketPublicCode(id, generated, clock.now()));
            result.put(id, generated);
        }
        return result;
    }
}
