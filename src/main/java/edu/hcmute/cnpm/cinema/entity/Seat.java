package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_seat", columnNames = {"room_id", "seat_row", "seat_column"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "seat_row", nullable = false, length = 5)
    private String seatRow;

    @Column(name = "seat_column", nullable = false)
    private Integer seatColumn;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType = "NORMAL"; // NORMAL | VIP | COUPLE

    public Seat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getSeatRow() { return seatRow; }
    public void setSeatRow(String seatRow) { this.seatRow = seatRow; }
    public Integer getSeatColumn() { return seatColumn; }
    public void setSeatColumn(Integer seatColumn) { this.seatColumn = seatColumn; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
}
