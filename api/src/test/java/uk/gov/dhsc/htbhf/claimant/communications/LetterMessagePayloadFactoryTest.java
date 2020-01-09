package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressAndPaymentFields;
import static uk.gov.dhsc.htbhf.claimant.communications.LetterMessagePayloadFactory.buildLetterPayloadWithAddressOnly;
import static uk.gov.dhsc.htbhf.claimant.message.LetterTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.UPDATE_YOUR_ADDRESS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;

@ExtendWith(MockitoExtension.class)
class LetterMessagePayloadFactoryTest {

    private static final Map.Entry[] ADDRESS_ENTRIES = {
            entry(ADDRESS_LINE_1.getTemplateKeyName(), HOMER_FORENAME + " " + SIMPSON_SURNAME),
            entry(ADDRESS_LINE_2.getTemplateKeyName(), SIMPSONS_ADDRESS_LINE_1),
            entry(ADDRESS_LINE_3.getTemplateKeyName(), SIMPSONS_ADDRESS_LINE_2),
            entry(ADDRESS_LINE_4.getTemplateKeyName(), SIMPSONS_TOWN),
            entry(ADDRESS_LINE_5.getTemplateKeyName(), SIMPSONS_COUNTY),
            entry(POSTCODE.getTemplateKeyName(), SIMPSONS_POSTCODE)
    };

    @Test
    void shouldCreatePayloadWithCorrectAddressOnly() {
        Claim claim = aValidClaim();

        LetterMessagePayload payload = buildLetterPayloadWithAddressOnly(claim, UPDATE_YOUR_ADDRESS);

        assertThat(payload).isNotNull();
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getLetterType()).isEqualTo(UPDATE_YOUR_ADDRESS);
        assertThat(payload.getPersonalisation()).containsOnly(ADDRESS_ENTRIES);
    }

    @Test
    void shouldCreatePayloadWithAddressAndPaymentFields() {
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = anEligibleDecision();

        LetterMessagePayload payload = buildLetterPayloadWithAddressAndPaymentFields(claim, decision, APPLICATION_SUCCESS_CHILDREN_MATCH);

        assertThat(payload).isNotNull();
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getLetterType()).isEqualTo(APPLICATION_SUCCESS_CHILDREN_MATCH);
        assertThat(payload.getPersonalisation()).contains(ADDRESS_ENTRIES);
        PaymentCycleVoucherEntitlement voucherEntitlement = decision.getVoucherEntitlement();
        int singleVoucherValueInPence = voucherEntitlement.getSingleVoucherValueInPence();
        assertThat(payload.getPersonalisation()).contains(
                entry("payment_amount", voucherEntitlement.getTotalVoucherValueInPence()),
                entry("pregnancy_payment", voucherEntitlement.getVouchersForPregnancy() * singleVoucherValueInPence),
                entry("children_under_1_payment", voucherEntitlement.getVouchersForChildrenUnderOne() * singleVoucherValueInPence),
                entry("children_under_4_payment", voucherEntitlement.getVouchersForChildrenBetweenOneAndFour() * singleVoucherValueInPence)
        );
    }
}
