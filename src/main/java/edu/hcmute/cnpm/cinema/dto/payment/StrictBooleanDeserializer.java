package edu.hcmute.cnpm.cinema.dto.payment;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

/** Chỉ nhận true/false trong JSON, không ép chuỗi hoặc số thành xác nhận. */
public class StrictBooleanDeserializer extends JsonDeserializer<Boolean> {
    @Override public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_TRUE) return true;
        if (parser.currentToken() == JsonToken.VALUE_FALSE) return false;
        return (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
    }
}
