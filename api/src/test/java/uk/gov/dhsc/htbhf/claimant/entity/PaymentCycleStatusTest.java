package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCycleStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ELIGIBLE, READY_FOR_PAYMENT",
            "INELIGIBLE, INELIGIBLE",
            "PENDING, INELIGIBLE",
            "NO_MATCH, INELIGIBLE",
            "ERROR, INELIGIBLE",
            "DUPLICATE, INELIGIBLE"
    })
    void getStatusForEligibilityDecision(EligibilityStatus eligibilityStatus, PaymentCycleStatus expectedPaymentCycleStatus) {
        //Given EligibilityStatus value in CsvSource
        //When
        PaymentCycleStatus paymentCycleStatus = PaymentCycleStatus.getStatusForEligibilityDecision(eligibilityStatus);
        //Then
        assertThat(paymentCycleStatus).isEqualTo(expectedPaymentCycleStatus);
    }
}
