package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.NO_HOUSEHOLD_IDENTIFIER_PROVIDED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityNotConfirmedResponse;

class EligibilityAndEntitlementDecisionFactoryTest {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private EligibilityAndEntitlementDecisionFactory factory = new EligibilityAndEntitlementDecisionFactory();
    private static final CombinedIdentityAndEligibilityResponse ELIGIBILITY_RESPONSE = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();

    @Test
    void shouldBuildDecisionWithoutExistingClaimUUID() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE, entitlement,  NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(ELIGIBILITY_RESPONSE)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForDuplicateClaim() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE, entitlement, DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(ELIGIBILITY_RESPONSE)
                .voucherEntitlement(null)
                .eligibilityStatus(EligibilityStatus.DUPLICATE)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForEligibleResponseWithoutVouchers() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE, entitlement, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(ELIGIBILITY_RESPONSE)
                .voucherEntitlement(null)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForIneligibleResponse() {
        //Given
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityNotConfirmedResponse();
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(identityAndEligibilityResponse, entitlement, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .dwpHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .hmrcHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .voucherEntitlement(null)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

}
