package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.StockMovementType;

/** Một lần nhân viên cập nhật kho: nhập thêm, xuất hủy hoặc ghi số kiểm kê. */
public class StockAdjustmentForm {
    private StockMovementType type = StockMovementType.IMPORT;
    private Integer quantity;
    private String note;

    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type = type; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
