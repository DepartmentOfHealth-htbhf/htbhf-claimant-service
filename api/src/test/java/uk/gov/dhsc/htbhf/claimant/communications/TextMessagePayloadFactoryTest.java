package uk.gov.dhsc.htbhf.claimant.communications;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextMessagePayload;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.payload.TextType.INSTANT_SUCCESS_TEXT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDate;

class TextMessagePayloadFactoryTest {

    private TextMessagePayloadFactory textMessagePayloadFactory = new TextMessagePayloadFactory();

    @Test
    void shouldBuildTextMessagePayloadWithAllVouchers() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(28);
        PaymentCycle paymentCycle = aPaymentCycleWithStartAndEndDate(startDate, endDate);

        TextMessagePayload payload
                = textMessagePayloadFactory.buildTextMessagePayload(paymentCycle.getClaim(), paymentCycle.getVoucherEntitlement(), INSTANT_SUCCESS_TEXT);

        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getTextType()).isEqualTo(INSTANT_SUCCESS_TEXT);
        assertThat(payload.getTextPersonalisation()).contains(
                Assertions.entry("payment_amount", "£49.60"),
                Assertions.entry("reference_number", "0E1567C0B2"));
    }
}
