package edu.hcmute.cnpm.cinema.dto.booking;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public record CancelHoldRequest(
        @JsonDeserialize(contentUsing = StrictIdDeserializer.class) List<Long> ticketIds) {}
