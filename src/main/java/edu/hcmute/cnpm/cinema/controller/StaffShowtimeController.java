package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.service.BookingClock;
import edu.hcmute.cnpm.cinema.service.DailyShowtimeService;
import edu.hcmute.cnpm.cinema.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Khu vực nhân viên: bảng suất chiếu trong ngày theo từng phòng, để điều phối khách vào và ra.
 *
 * Quyền truy cập do {@link StaffAccessInterceptor} lo (nhân viên và quản trị viên).
 */
@Controller
@RequestMapping("/nhan-vien/suat-chieu")
public class StaffShowtimeController {

    private final DailyShowtimeService dailyShowtimeService;
    private final RoomService roomService;
    private final BookingClock clock;

    public StaffShowtimeController(DailyShowtimeService dailyShowtimeService, RoomService roomService,
                                   BookingClock clock) {
        this.dailyShowtimeService = dailyShowtimeService;
        this.roomService = roomService;
        this.clock = clock;
    }

    /**
     * {@code ?ngay=2026-10-03&phong=3}; bỏ trống ngày là hôm nay, bỏ trống phòng là mọi phòng.
     * Dùng GET để nhân viên bấm F5 là cập nhật trạng thái, gửi link cho đồng nghiệp được.
     */
    @GetMapping
    public String showDay(@RequestParam(name = "ngay", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam(name = "phong", required = false) Long roomId,
                          Model model) {
        LocalDateTime now = clock.now();
        LocalDate day = date == null ? now.toLocalDate() : date;
        List<Room> rooms = roomService.findAllRooms().stream()
                .sorted(Comparator.comparing(Room::getId))
                .toList();

        model.addAttribute("board", dailyShowtimeService.buildBoard(day, roomId, now));
        model.addAttribute("rooms", rooms);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("today", now.toLocalDate());
        model.addAttribute("now", now);
        return "staff/showtime-day";
    }
}
