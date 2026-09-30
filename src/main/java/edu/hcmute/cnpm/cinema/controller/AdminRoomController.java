package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.RoomForm;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {
    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findAllRooms());
        return "movie/room-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Room room = roomService.findById(id);
        List<Seat> seats = roomService.findSeatsByRoom(id);
        model.addAttribute("room", room);
        model.addAttribute("seats", seats);
        model.addAttribute("normalSeatCount", seats.stream()
                .filter(seat -> "NORMAL".equals(seat.getSeatType())).count());
        model.addAttribute("vipSeatCount", seats.stream()
                .filter(seat -> "VIP".equals(seat.getSeatType())).count());
        return "movie/room-detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("roomForm", new RoomForm());
        return "movie/room-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roomForm") RoomForm form, BindingResult errors,
                         Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "movie/room-form";
        }
        try {
            roomService.createRoom(form.getName(), form.getTotalRows(), form.getTotalColumns());
        } catch (BusinessException exception) {
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "movie/room-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã thêm phòng và sinh ghế thành công.");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("roomForm", RoomForm.from(roomService.findById(id)));
        model.addAttribute("roomId", id);
        return "movie/room-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("roomForm") RoomForm form,
                         BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("roomId", id);
            return "movie/room-form";
        }
        try {
            roomService.updateRoom(id, form.getName(), form.getTotalRows(), form.getTotalColumns());
        } catch (BusinessException exception) {
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            model.addAttribute("roomId", id);
            return "movie/room-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã cập nhật phòng chiếu.");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã xoá phòng chiếu.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/admin/rooms";
    }
}
