package edu.hcmute.cnpm.cinema.dto.payment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.hcmute.cnpm.cinema.dto.booking.StrictIdDeserializer;

/** Giá từ khách chỉ dùng đối chiếu, không dùng tự tính tiền. */
public record DemoWalletRequest(String token,
        @JsonDeserialize(using = StrictIdDeserializer.class) Long expectedAmount,
        @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean confirmed) {}
