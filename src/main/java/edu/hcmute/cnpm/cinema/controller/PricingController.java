package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.service.TicketPricingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Trang bảng giá vé công bố của rạp. */
@Controller
@RequestMapping("/gia-ve")
public class PricingController {

    private final TicketPricingService ticketPricingService;

    public PricingController(TicketPricingService ticketPricingService) {
        this.ticketPricingService = ticketPricingService;
    }

    @GetMapping
    public String showPriceList(Model model) {
        model.addAttribute("priceRows", ticketPricingService.findPriceRows());
        model.addAttribute("seatSurcharges", ticketPricingService.findSeatSurcharges());
        model.addAttribute("exampleBasePrice", ticketPricingService.getExampleBasePrice());
        model.addAttribute("upcomingShowtimeCount", ticketPricingService.countUpcomingShowtimes());
        return "pricing/price-list";
    }
}
