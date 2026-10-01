package edu.hcmute.cnpm.cinema.dto.booking;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public class HoldSeatsRequest {
    @JsonDeserialize(contentUsing = StrictIdDeserializer.class)
    private List<Long> seatIds;
    @JsonDeserialize(contentUsing = StrictIdDeserializer.class)
    private List<Long> expectedTicketIds;
    private boolean ageConfirmed;
    private boolean termsAccepted;
    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> value) { seatIds = value; }
    public List<Long> getExpectedTicketIds() { return expectedTicketIds; }
    public void setExpectedTicketIds(List<Long> value) { expectedTicketIds = value; }
    public boolean isAgeConfirmed() { return ageConfirmed; }
    public void setAgeConfirmed(boolean value) { ageConfirmed = value; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean value) { termsAccepted = value; }
}
