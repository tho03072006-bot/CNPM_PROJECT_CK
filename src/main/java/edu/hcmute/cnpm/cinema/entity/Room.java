package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "total_columns", nullable = false)
    private Integer totalColumns;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<Seat> seats = new ArrayList<>();

    public Room() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getTotalColumns() { return totalColumns; }
    public void setTotalColumns(Integer totalColumns) { this.totalColumns = totalColumns; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}
