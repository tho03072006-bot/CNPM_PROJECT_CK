package edu.hcmute.cnpm.cinema.controller.form;

import java.util.HashMap;
import java.util.Map;

/** Số lượng bắp nước khách chọn, khóa là mã sản phẩm. */
public class ConcessionSelectionForm {
    private Map<Long, Integer> quantities = new HashMap<>();

    public Map<Long, Integer> getQuantities() { return quantities; }
    public void setQuantities(Map<Long, Integer> quantities) { this.quantities = quantities; }
}
