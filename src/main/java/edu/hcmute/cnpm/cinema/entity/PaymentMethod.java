package edu.hcmute.cnpm.cinema.entity;
/** Cách trả tiền vé; giao dịch giả lập có phương thức riêng. */
public enum PaymentMethod {
    COUNTER("Tiền mặt tại quầy"),
    MOMO("Ví MoMo"),
    MOMO_DEMO("MoMo giả lập Nhóm 8 — không phát sinh tiền thật");
    private final String displayName;
    PaymentMethod(String displayName){this.displayName=displayName;}
    public String getDisplayName(){return displayName;}
}
