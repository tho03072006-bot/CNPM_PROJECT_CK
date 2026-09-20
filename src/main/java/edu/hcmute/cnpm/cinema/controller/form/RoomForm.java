package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RoomForm {
    @NotBlank(message = "Vui lòng nhập tên phòng.")
    @Size(max = 50, message = "Tên phòng không được dài quá 50 ký tự.")
    private String name;
    @NotNull(message = "Vui lòng nhập số hàng.")
    @Min(value = 1, message = "Số hàng phải lớn hơn 0.")
    @Max(value = 26, message = "Tối đa 26 hàng, từ A đến Z.")
    private Integer totalRows;
    @NotNull(message = "Vui lòng nhập số cột.")
    @Min(value = 1, message = "Số cột phải lớn hơn 0.")
    @Max(value = 50, message = "Tối đa 50 cột.")
    private Integer totalColumns;

    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();
        form.name = room.getName();
        form.totalRows = room.getTotalRows();
        form.totalColumns = room.getTotalColumns();
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getTotalColumns() { return totalColumns; }
    public void setTotalColumns(Integer totalColumns) { this.totalColumns = totalColumns; }
}
