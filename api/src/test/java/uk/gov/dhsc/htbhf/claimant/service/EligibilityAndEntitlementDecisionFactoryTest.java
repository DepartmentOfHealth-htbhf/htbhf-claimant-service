package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aValidDecisionBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.IdAndEligibilityResponseTestDataFactory.anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithZeroVouchers;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.NO_HOUSEHOLD_IDENTIFIER_PROVIDED;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityNotConfirmedResponse;

class EligibilityAndEntitlementDecisionFactoryTest {

    private static final boolean NOT_DUPLICATE = false;
    private static final boolean DUPLICATE = true;
    private EligibilityAndEntitlementDecisionFactory factory = new EligibilityAndEntitlementDecisionFactory();
    private IdentityAndEligibilityResponse eligibilityResponse = anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier();
    private static final UUID EXISTING_CLAIM_UUID = UUID.randomUUID();

    @Test
    void shouldBuildDecisionWithNoHmrcHouseholdIdentifier() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(eligibilityResponse, entitlement, EXISTING_CLAIM_UUID,
                null, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(eligibilityResponse)
                .existingClaimId(EXISTING_CLAIM_UUID)
                .hmrcHouseholdIdentifier(null)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionWithHmrcHouseholdIdentifier() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(eligibilityResponse, entitlement, EXISTING_CLAIM_UUID,
                HMRC_HOUSEHOLD_IDENTIFIER, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(eligibilityResponse)
                .existingClaimId(EXISTING_CLAIM_UUID)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionWithoutExistingClaimUUID() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(eligibilityResponse,
                entitlement, null, null, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(eligibilityResponse)
                .hmrcHouseholdIdentifier(null)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForDuplicateClaim() {
        //Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(eligibilityResponse,
                entitlement, null, null, DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .identityAndEligibilityResponse(eligibilityResponse)
                .hmrcHouseholdIdentifier(null)
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
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(eligibilityResponse,
                entitlement, EXISTING_CLAIM_UUID, null, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(eligibilityResponse)
                .hmrcHouseholdIdentifier(null)
                .voucherEntitlement(null)
                .existingClaimId(EXISTING_CLAIM_UUID)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

    @Test
    void shouldBuildDecisionForIneligibleResponse() {
        //Given
        IdentityAndEligibilityResponse identityAndEligibilityResponse = anIdentityMatchedEligibilityNotConfirmedResponse();
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithZeroVouchers();

        //When
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = factory.buildDecision(identityAndEligibilityResponse, entitlement,
                EXISTING_CLAIM_UUID, null, NOT_DUPLICATE);

        //Then
        EligibilityAndEntitlementDecision expectedDecision = aValidDecisionBuilder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .dwpHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_PROVIDED)
                .hmrcHouseholdIdentifier(null)
                .voucherEntitlement(null)
                .dateOfBirthOfChildren(emptyList())
                .existingClaimId(EXISTING_CLAIM_UUID)
                .build();
        assertThat(eligibilityAndEntitlementDecision).isEqualTo(expectedDecision);
    }

}
