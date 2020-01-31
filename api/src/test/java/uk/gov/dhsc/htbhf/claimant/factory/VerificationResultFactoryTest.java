package uk.gov.dhsc.htbhf.claimant.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
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
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDateAndChildrenDob;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

class VerificationResultFactoryTest {

    @Test
    void shouldCreateVerificationResult() {
        Claimant claimant = aValidClaimant();
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches();

        VerificationResult result = VerificationResultFactory.buildVerificationResult(claimant, identityAndEligibilityResponse);

        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1Match()).isEqualTo(identityAndEligibilityResponse.getAddressLine1Match());
        assertThat(result.getDeathVerificationFlag()).isEqualTo(identityAndEligibilityResponse.getDeathVerificationFlag());
        assertThat(result.getEligibilityOutcome()).isEqualTo(identityAndEligibilityResponse.getEligibilityStatus());
        assertThat(result.getEmailAddressMatch()).isEqualTo(identityAndEligibilityResponse.getEmailAddressMatch());
        assertThat(result.getIdentityOutcome()).isEqualTo(identityAndEligibilityResponse.getIdentityStatus());
        assertThat(result.getMobilePhoneMatch()).isEqualTo(identityAndEligibilityResponse.getMobilePhoneMatch());
        assertThat(result.getPostcodeMatch()).isEqualTo(identityAndEligibilityResponse.getPostcodeMatch());
        assertThat(result.getPregnantChildDOBMatch()).isEqualTo(identityAndEligibilityResponse.getPregnantChildDOBMatch());
        assertThat(result.getQualifyingReason()).isEqualTo(identityAndEligibilityResponse.getQualifyingReason());
    }

    @ParameterizedTest
    @MethodSource("noChildMatches")
    void shouldSetIsPregnantOrChildMatchedToTrueWhenPregnant(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claimant claimant = aClaimantWithExpectedDeliveryDateAndChildrenDob(LocalDate.now(), declaredChildren);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        VerificationResult result = VerificationResultFactory.buildVerificationResult(claimant, identityAndEligibilityResponse);

        assertThat(result.getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("atLeastOneChildMatches")
    void shouldSetIsPregnantOrChildMatchedToTrueWhenChildMatchesAndNotPregnant(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claimant claimant = aClaimantWithExpectedDeliveryDateAndChildrenDob(null, declaredChildren);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        VerificationResult result = VerificationResultFactory.buildVerificationResult(claimant, identityAndEligibilityResponse);

        assertThat(result.getIsPregnantOrAtLeast1ChildMatched()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("noChildMatches")
    void shouldSetIsPregnantOrChildMatchedToFalse(List<LocalDate> declaredChildren, List<LocalDate> registeredChildren) {
        Claimant claimant = aClaimantWithExpectedDeliveryDateAndChildrenDob(null, declaredChildren);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(registeredChildren);

        VerificationResult result = VerificationResultFactory.buildVerificationResult(claimant, identityAndEligibilityResponse);

        assertThat(result.getIsPregnantOrAtLeast1ChildMatched()).isFalse();
    }

    @Test
    void shouldSetIsPregnantOrChildMatchedToTrueWhenPregnantWithChildren() {
        Claimant claimant = aClaimantWithExpectedDeliveryDateAndChildrenDob(null, singletonList(EXACTLY_SIX_MONTH_OLD));
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse =
                anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(singletonList(EXACTLY_SIX_MONTH_OLD));

        VerificationResult result = VerificationResultFactory.buildVerificationResult(claimant, identityAndEligibilityResponse);

        assertThat(result.getIsPregnantOrAtLeast1ChildMatched()).isTrue();
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
