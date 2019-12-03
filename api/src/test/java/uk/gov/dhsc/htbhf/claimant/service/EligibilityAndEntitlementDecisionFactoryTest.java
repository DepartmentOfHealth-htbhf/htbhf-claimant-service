package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdentityAndEligibilityResponseTestDataFactory;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.NO_HOUSEHOLD_IDENTIFIER_PROVIDED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;

class EligibilityAndEntitlementDecisionFactoryTest {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private EligibilityAndEntitlementDecisionFactory factory = new EligibilityAndEntitlementDecisionFactory();
    private static final CombinedIdentityAndEligibilityResponse ELIGIBILITY_RESPONSE = CombinedIdentityAndEligibilityResponseTestDataFactory
            .anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches();
    private static final UUID EXISTING_CLAIM_UUID = UUID.randomUUID();
    private static final UUID NO_EXISTING_CLAIM_UUID = null;

    @Test
    void shouldBuildDecisionWithoutExistingClaimUUID() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE,
                entitlement, NO_EXISTING_CLAIM_UUID, NOT_DUPLICATE);

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
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE,
                entitlement, NO_EXISTING_CLAIM_UUID, DUPLICATE);

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
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(ELIGIBILITY_RESPONSE,
                entitlement, EXISTING_CLAIM_UUID, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(ELIGIBILITY_RESPONSE)
                .voucherEntitlement(null)
                .existingClaimId(EXISTING_CLAIM_UUID)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForIneligibleResponse() {
        //Given
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = CombinedIdentityAndEligibilityResponseTestDataFactory
                .anIdentityMatchedEligibilityNotConfirmedResponse();
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(identityAndEligibilityResponse, entitlement,
                EXISTING_CLAIM_UUID, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .dwpHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .hmrcHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .voucherEntitlement(null)
                .dateOfBirthOfChildren(emptyList())
                .existingClaimId(EXISTING_CLAIM_UUID)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

}
