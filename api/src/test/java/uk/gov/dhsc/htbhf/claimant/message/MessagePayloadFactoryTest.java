package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aValidPaymentCycleVoucherEntitlement;

class MessagePayloadFactoryTest {

    @Test
    void shouldCreateNewCardMessagePayload() {
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = aValidPaymentCycleVoucherEntitlement();

        NewCardRequestMessagePayload payload = MessagePayloadFactory.buildNewCardMessagePayload(claim, voucherEntitlement);

        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getVoucherEntitlement()).isEqualTo(voucherEntitlement);
    }

}
