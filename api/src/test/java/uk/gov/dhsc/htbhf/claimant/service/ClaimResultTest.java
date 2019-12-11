package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.EXACTLY_SIX_MONTH_OLD;
import static uk.gov.dhsc.htbhf.TestConstants.EXACTLY_THREE_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDateAndChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

class ClaimResultTest {

    @Test
    void shouldCreateResultWithEntitlement() {
        Claim claim = aValidClaim();
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();

        ClaimResult result = ClaimResult.withEntitlement(claim, voucherEntitlement, identityAndEligibilityResponse);

        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isEqualTo(claim);
        VerificationResult verificationResult = result.getVerificationResult();
        assertThat(verificationResult.getAddressLine1Match()).isEqualTo(identityAndEligibilityResponse.getAddressLine1Match());
        assertThat(verificationResult.getDeathVerificationFlag()).isEqualTo(identityAndEligibilityResponse.getDeathVerificationFlag());
        assertThat(verificationResult.getEligibilityOutcome()).isEqualTo(identityAndEligibilityResponse.getEligibilityStatus());
        assertThat(verificationResult.getEmailAddressMatch()).isEqualTo(identityAndEligibilityResponse.getEmailAddressMatch());
        assertThat(verificationResult.getIdentityOutcome()).isEqualTo(identityAndEligibilityResponse.getIdentityStatus());
        assertThat(verificationResult.getMobilePhoneMatch()).isEqualTo(identityAndEligibilityResponse.getMobilePhoneMatch());
        assertThat(verificationResult.getPostcodeMatch()).isEqualTo(identityAndEligibilityResponse.getPostcodeMatch());
        assertThat(verificationResult.getPregnantChildDOBMatch()).isEqualTo(identityAndEligibilityResponse.getPregnantChildDOBMatch());
        assertThat(verificationResult.getQualifyingBenefits()).isEqualTo(identityAndEligibilityResponse.getQualifyingBenefits());
    }

    @ParameterizedTest
    @MethodSource("noChildMatches")
    void shouldSetIsPregnantOrChildMatchedToTrueWhenPregnant(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claim claim = aClaimWithExpectedDeliveryDateAndChildrenDobs(LocalDate.now(), declaredChildren);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        ClaimResult result = ClaimResult.withEntitlement(claim, voucherEntitlement, identityAndEligibilityResponse);

        assertThat(result.getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("atLeastOneChildMatches")
    void shouldSetIsPregnantOrChildMatchedToTrueWhenChildMatchesAndNotPregnant(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claim claim = aClaimWithExpectedDeliveryDateAndChildrenDobs(null, declaredChildren);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        ClaimResult result = ClaimResult.withEntitlement(claim, voucherEntitlement, identityAndEligibilityResponse);

        assertThat(result.getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("noChildMatches")
    void shouldSetIsPregnantOrChildMatchedToFalse(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claim claim = aClaimWithExpectedDeliveryDateAndChildrenDobs(null, declaredChildren);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        ClaimResult result = ClaimResult.withEntitlement(claim, voucherEntitlement, identityAndEligibilityResponse);

        assertThat(result.getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isFalse();
    }

    @Test
    void shouldSetIsPregnantOrChildMatchedToTrueWhenPregnantWithChildren() {
        Claim claim = aClaimWithExpectedDeliveryDateAndChildrenDobs(LocalDate.now(), singletonList(EXACTLY_SIX_MONTH_OLD));
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse =
                anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(singletonList(EXACTLY_SIX_MONTH_OLD));

        ClaimResult result = ClaimResult.withEntitlement(claim, voucherEntitlement, identityAndEligibilityResponse);

        assertThat(result.getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    static Stream<Arguments> atLeastOneChildMatches() {
        return Stream.of(
                Arguments.of(singletonList(EXACTLY_SIX_MONTH_OLD), singletonList(EXACTLY_SIX_MONTH_OLD)),
                Arguments.of(List.of(EXACTLY_SIX_MONTH_OLD, EXACTLY_THREE_YEAR_OLD), singletonList(EXACTLY_SIX_MONTH_OLD)),
                Arguments.of(singletonList(EXACTLY_SIX_MONTH_OLD), List.of(EXACTLY_SIX_MONTH_OLD, EXACTLY_THREE_YEAR_OLD))
        );
    }

    static Stream<Arguments> noChildMatches() {
        return Stream.of(
                Arguments.of(null, emptyList()),
                Arguments.of(singletonList(EXACTLY_SIX_MONTH_OLD), emptyList()),
                Arguments.of(emptyList(), singletonList(EXACTLY_SIX_MONTH_OLD)),
                Arguments.of(singletonList(EXACTLY_SIX_MONTH_OLD), singletonList(EXACTLY_THREE_YEAR_OLD))
        );
    }

}