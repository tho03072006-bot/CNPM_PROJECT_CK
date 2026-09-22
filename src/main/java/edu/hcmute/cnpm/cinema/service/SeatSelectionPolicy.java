package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Kiểm tra cách chọn ghế trước khi hệ thống tạo vé giữ chỗ. */
@Service
public class SeatSelectionPolicy {

    public static final int MAX_ADMISSIONS_PER_BOOKING = 8;

    public void validateSelection(List<Seat> roomSeats, List<Seat> selectedSeats,
                                  Set<Long> unavailableSeatIds) {
        validateMaximumAdmissions(selectedSeats);
        validateSeatsAreAdjacent(selectedSeats);
        validateNoNewSingleSeatGap(roomSeats, selectedSeats, unavailableSeatIds);
    }

    public int getMaximumAdmissionsPerBooking() {
        return MAX_ADMISSIONS_PER_BOOKING;
    }

    private void validateMaximumAdmissions(List<Seat> selectedSeats) {
        int admissionCount = selectedSeats.stream().mapToInt(this::resolveSeatCapacity).sum();
        if (admissionCount > MAX_ADMISSIONS_PER_BOOKING) {
            throw new InvalidBookingException("Mỗi lượt chỉ được đặt tối đa "
                    + MAX_ADMISSIONS_PER_BOOKING + " chỗ. Ghế đôi được tính là hai chỗ.");
        }
    }

    private void validateSeatsAreAdjacent(List<Seat> selectedSeats) {
        if (selectedSeats.size() <= 1) {
            return;
        }
        String selectedRow = selectedSeats.getFirst().getSeatRow();
        if (selectedRow == null || selectedSeats.stream()
                .anyMatch(seat -> !selectedRow.equals(seat.getSeatRow()))) {
            throw new InvalidBookingException(
                    "Các ghế trong cùng lượt đặt phải nằm trên cùng một hàng.");
        }

        List<Seat> orderedSeats = selectedSeats.stream()
                .sorted(Comparator.comparing(Seat::getSeatColumn))
                .toList();
        for (int index = 1; index < orderedSeats.size(); index++) {
            Integer previousColumn = orderedSeats.get(index - 1).getSeatColumn();
            Integer currentColumn = orderedSeats.get(index).getSeatColumn();
            if (previousColumn == null || currentColumn == null || currentColumn != previousColumn + 1) {
                throw new InvalidBookingException(
                        "Các ghế trong cùng lượt đặt phải liền nhau, không được bỏ trống ghế ở giữa.");
            }
        }
    }

    private void validateNoNewSingleSeatGap(List<Seat> roomSeats, List<Seat> selectedSeats,
                                             Set<Long> unavailableSeatIds) {
        Set<Long> originalGaps = findSingleSeatGaps(roomSeats, unavailableSeatIds);
        Set<Long> seatsAfterSelection = new HashSet<>(unavailableSeatIds);
        selectedSeats.stream().map(Seat::getId).forEach(seatsAfterSelection::add);

        Set<Long> newGaps = findSingleSeatGaps(roomSeats, seatsAfterSelection);
        newGaps.removeAll(originalGaps);
        if (newGaps.isEmpty()) {
            return;
        }

        Long gapSeatId = newGaps.iterator().next();
        Seat gapSeat = roomSeats.stream()
                .filter(seat -> gapSeatId.equals(seat.getId()))
                .findFirst()
                .orElse(null);
        String seatLabel = gapSeat == null ? "một ghế" : "ghế " + formatSeatLabel(gapSeat);
        throw new InvalidBookingException("Lựa chọn này sẽ để " + seatLabel
                + " trống một mình. Vui lòng chọn thêm ghế đó hoặc chọn vị trí khác.");
    }

    private Set<Long> findSingleSeatGaps(List<Seat> roomSeats, Set<Long> unavailableSeatIds) {
        Map<String, List<Seat>> seatsByRow = new HashMap<>();
        for (Seat seat : roomSeats) {
            if (seat.getId() == null || seat.getSeatRow() == null || seat.getSeatColumn() == null) {
                continue;
            }
            seatsByRow.computeIfAbsent(seat.getSeatRow(), ignored -> new ArrayList<>()).add(seat);
        }

        Set<Long> singleSeatGaps = new HashSet<>();
        for (List<Seat> rowSeats : seatsByRow.values()) {
            rowSeats.sort(Comparator.comparing(Seat::getSeatColumn));
            int index = 0;
            while (index < rowSeats.size()) {
                Seat firstAvailableSeat = rowSeats.get(index);
                if (unavailableSeatIds.contains(firstAvailableSeat.getId())) {
                    index++;
                    continue;
                }

                int runEnd = index;
                while (runEnd + 1 < rowSeats.size()
                        && rowSeats.get(runEnd + 1).getSeatColumn()
                        == rowSeats.get(runEnd).getSeatColumn() + 1
                        && !unavailableSeatIds.contains(rowSeats.get(runEnd + 1).getId())) {
                    runEnd++;
                }
                if (runEnd == index) {
                    singleSeatGaps.add(firstAvailableSeat.getId());
                }
                index = runEnd + 1;
            }
        }
        return singleSeatGaps;
    }

    private int resolveSeatCapacity(Seat seat) {
        return "COUPLE".equals(seat.getSeatType()) ? 2 : 1;
    }

    private String formatSeatLabel(Seat seat) {
        return seat.getSeatRow() + seat.getSeatColumn();
    }
}
