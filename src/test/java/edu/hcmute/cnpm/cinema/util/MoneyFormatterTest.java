package edu.hcmute.cnpm.cinema.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatterTest {
    @ParameterizedTest
    @CsvSource({
            "0.00, 0 đ", "1, 1 đ", "999, 999 đ", "1000.00, 1.000 đ",
            "200000.00, 200.000 đ", "1234567.00, 1.234.567 đ",
            "-20000.00, -20.000 đ", "200000.49, 200.000 đ", "200000.50, 200.001 đ"
    })
    void shouldFormatVndWithoutDecimalPlaces(String input, String expected) {
        BigDecimal amount = new BigDecimal(input);
        assertThat(MoneyFormatter.format(amount)).isEqualTo(expected);
        assertThat(new MoneyFormatter().number(amount) + " đ").isEqualTo(expected);
        assertThat(amount.toPlainString()).isEqualTo(input);
    }

    @Test
    void shouldFormatMissingAndLargeAmountsWithoutConvertingToLong() {
        assertThat(MoneyFormatter.format(null)).isEqualTo("0 đ");
        assertThat(MoneyFormatter.format(new BigInteger("123456789012345678901")))
                .isEqualTo("123.456.789.012.345.678.901 đ");
        assertThat(MoneyFormatter.format(200000L)).isEqualTo("200.000 đ");
    }
}
