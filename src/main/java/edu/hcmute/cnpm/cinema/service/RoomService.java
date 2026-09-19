package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.RoomRepository;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public RoomService(RoomRepository roomRepository, SeatRepository seatRepository,
                       ShowtimeRepository showtimeRepository) {
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Room findById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng chiếu", roomId));
    }

    @Transactional
    public Room createRoom(String name, Integer totalRows, Integer totalColumns) {
        validate(name, totalRows, totalColumns);
        Room room = new Room();
        room.setName(name.trim());
        room.setTotalRows(totalRows);
        room.setTotalColumns(totalColumns);
        room = roomRepository.save(room);
        generateSeats(room.getId());
        return room;
    }

    @Transactional
    public Room updateRoom(Long roomId, String name, Integer totalRows, Integer totalColumns) {
        validate(name, totalRows, totalColumns);
        Room room = findById(roomId);
        boolean resized = !Objects.equals(room.getTotalRows(), totalRows)
                || !Objects.equals(room.getTotalColumns(), totalColumns);
        if (resized && showtimeRepository.existsByRoomId(roomId)) {
            throw new BusinessException("Không thể đổi sơ đồ ghế của phòng đã có suất chiếu.");
        }
        room.setName(name.trim());
        if (resized) {
            seatRepository.deleteAllInBatch(seatRepository.findByRoomId(roomId));
            room.setTotalRows(totalRows);
            room.setTotalColumns(totalColumns);
            generateSeats(roomId);
        }
        return room;
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = findById(roomId);
        if (showtimeRepository.existsByRoomId(roomId)) {
            throw new BusinessException("Không thể xoá phòng đang có suất chiếu.");
        }
        roomRepository.delete(room);
    }

    /** Gọi lại an toàn: chỉ thêm những ghế còn thiếu trong phòng. */
    @Transactional
    public void generateSeats(Long roomId) {
        Room room = findById(roomId);
        Set<String> existing = new HashSet<>();
        for (Seat seat : seatRepository.findByRoomId(roomId)) {
            existing.add(seat.getSeatRow() + ":" + seat.getSeatColumn());
        }
        for (int row = 0; row < room.getTotalRows(); row++) {
            String label = String.valueOf((char) ('A' + row));
            for (int column = 1; column <= room.getTotalColumns(); column++) {
                if (existing.add(label + ":" + column)) {
                    Seat seat = new Seat();
                    seat.setRoom(room);
                    seat.setSeatRow(label);
                    seat.setSeatColumn(column);
                    seat.setSeatType(row >= room.getTotalRows() - 2 ? "VIP" : "NORMAL");
                    seatRepository.save(seat);
                }
            }
        }
    }

    private void validate(String name, Integer rows, Integer columns) {
        if (name == null || name.isBlank() || name.trim().length() > 50) {
            throw new BusinessException("Tên phòng phải có từ 1 đến 50 ký tự.");
        }
        if (rows == null || rows < 1 || rows > 26) {
            throw new BusinessException("Số hàng phải từ 1 đến 26.");
        }
        if (columns == null || columns < 1 || columns > 50) {
            throw new BusinessException("Số cột phải từ 1 đến 50.");
        }
    }
}
