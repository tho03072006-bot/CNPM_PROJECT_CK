package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromotionController {
    private final PromotionService promotions;
    public PromotionController(PromotionService promotions) { this.promotions = promotions; }

    @GetMapping("/uu-dai")
    public String offers(Model model) {
        model.addAttribute("offers", promotions.availableOffers());
        return "account/offers";
    }
}
