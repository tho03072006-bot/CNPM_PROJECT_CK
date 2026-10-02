package edu.hcmute.cnpm.cinema.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dòng "Số tiền viết bằng chữ" trên hóa đơn. Unit test, không cần database. */
@DisplayName("Đọc số tiền thành chữ trên hóa đơn")
class VietnameseMoneyWordsTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "0             | Không đồng.",
            "15            | Mười lăm đồng.",
            "21            | Hai mươi mốt đồng.",
            "101           | Một trăm linh một đồng.",
            "1005          | Một nghìn không trăm linh năm đồng.",
            "55000         | Năm mươi lăm nghìn đồng chẵn.",
            "105500        | Một trăm linh năm nghìn năm trăm đồng.",
            "233000        | Hai trăm ba mươi ba nghìn đồng chẵn.",
            "280000        | Hai trăm tám mươi nghìn đồng chẵn.",
            "1015000       | Một triệu không trăm mười lăm nghìn đồng chẵn.",
            "2000000000    | Hai tỷ đồng chẵn.",
            "1000000001    | Một tỷ không trăm linh một đồng."
    })
    void shouldReadAmountLikeAccountingDocuments(long amount, String expected) {
        assertThat(VietnameseMoneyWords.of(BigDecimal.valueOf(amount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("Tiền có phần lẻ thập phân thì làm tròn về đồng trước khi đọc")
    void shouldRoundToWholeDong() {
        assertThat(VietnameseMoneyWords.of(new BigDecimal("79000.00"))).isEqualTo("Bảy mươi chín nghìn đồng chẵn.");
    }

    @Test
    @DisplayName("Số tiền âm là lỗi dữ liệu, không in lên hóa đơn")
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> VietnameseMoneyWords.of(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
