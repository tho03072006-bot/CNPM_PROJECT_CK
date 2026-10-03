package edu.hcmute.cnpm.cinema.service;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
@Component
public class TicketCodeGenerator {
    private final SecureRandom random = new SecureRandom();
    public String nextCode() { return Integer.toString(10_000_000 + random.nextInt(90_000_000)); }
}
