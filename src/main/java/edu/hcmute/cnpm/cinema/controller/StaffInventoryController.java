package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.StockAdjustmentForm;
import edu.hcmute.cnpm.cinema.entity.ConcessionStockMovement;
import edu.hcmute.cnpm.cinema.entity.StockMovementType;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.ConcessionInventoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Kho bắp nước cho nhân viên và quản lý: xem tồn kho, nhập kho, xuất hủy, kiểm kê.
 *
 * Quyền truy cập do {@link StaffAccessInterceptor} chặn theo đường dẫn /nhan-vien/**,
 * Service kiểm tra vai trò thêm một lần trước khi ghi kho.
 */
@Controller
@RequestMapping("/nhan-vien/kho-bap-nuoc")
public class StaffInventoryController {

    private static final List<StockMovementType> MANUAL_TYPES =
            List.of(StockMovementType.IMPORT, StockMovementType.WRITE_OFF, StockMovementType.STOCKTAKE);

    private final ConcessionInventoryService inventoryService;

    public StaffInventoryController(ConcessionInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("inventory", inventoryService.overview());
        model.addAttribute("movements", inventoryService.findRecentMovements());
        return "staff/inventory-list";
    }

    @GetMapping("/{productId}")
    public String detail(@PathVariable Long productId, Model model) {
        model.addAttribute("item", inventoryService.findStockItem(productId));
        model.addAttribute("movements", inventoryService.findMovements(productId));
        model.addAttribute("movementTypes", MANUAL_TYPES);
        if (!model.containsAttribute("adjustmentForm")) {
            model.addAttribute("adjustmentForm", new StockAdjustmentForm());
        }
        return "staff/inventory-detail";
    }

    @PostMapping("/{productId}/dieu-chinh")
    public String adjust(@PathVariable Long productId,
                         @ModelAttribute("adjustmentForm") StockAdjustmentForm form, BindingResult bindingResult,
                         HttpSession session, RedirectAttributes redirectAttributes) {
        User staff = SessionUsers.current(session);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, "Số lượng phải là số nguyên, ví dụ 50.");
            return "redirect:/nhan-vien/kho-bap-nuoc/" + productId;
        }
        try {
            ConcessionStockMovement movement = inventoryService.adjustStock(
                    productId, staff.getId(), form.getType(), form.getQuantity(), form.getNote());
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, successMessage(movement));
        } catch (BusinessException exception) {
            // Giữ lại số vừa nhập để nhân viên sửa, không phải gõ lại từ đầu.
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            redirectAttributes.addFlashAttribute("adjustmentForm", form);
        }
        return "redirect:/nhan-vien/kho-bap-nuoc/" + productId;
    }

    @PostMapping("/{productId}/nguong")
    public String updateThreshold(@PathVariable Long productId,
                                  @RequestParam(name = "lowStockThreshold", required = false) Integer threshold,
                                  HttpSession session, RedirectAttributes redirectAttributes) {
        User staff = SessionUsers.current(session);
        try {
            inventoryService.updateLowStockThreshold(productId, staff.getId(), threshold);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã lưu ngưỡng cảnh báo: còn " + threshold + " phần trở xuống sẽ báo sắp hết.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/nhan-vien/kho-bap-nuoc/" + productId;
    }

    private static String successMessage(ConcessionStockMovement movement) {
        int change = movement.getQuantityChange();
        String after = " Tồn kho hiện tại: " + movement.getQuantityAfter() + " phần.";
        return switch (movement.getType()) {
            case IMPORT -> "Đã nhập thêm " + change + " phần." + after;
            case WRITE_OFF -> "Đã xuất hủy " + (-change) + " phần." + after;
            default -> change == 0
                    ? "Kiểm kê khớp sổ." + after
                    : "Đã ghi kiểm kê, lệch " + (change > 0 ? "+" : "") + change + " phần so với sổ." + after;
        };
    }
}
