package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

    @ParameterizedTest
    @CsvSource(
            value = {
                    "1234: £12.34",
                    "5: £0.05",
                    "0: £0.00",
                    "78456789: £784,567.89",
                    "1239876543: £12,398,765.43",
                    "-572: -£5.72"
            },
            delimiter = ':'
    )
    void shouldConvertPenceToPounds(int amountInPence, String amountInPounds) {
        assertThat(MoneyUtils.convertPenceToPounds(amountInPence)).isEqualTo(amountInPounds);
    }

}
