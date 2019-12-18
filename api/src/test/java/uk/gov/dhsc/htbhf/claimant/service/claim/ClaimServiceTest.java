package uk.gov.dhsc.htbhf.claimant.service.claim;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_SIX_MONTH_OLD;
import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_THREE_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildDuplicateDecisionWithExistingClaimId;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.DEVICE_FINGERPRINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestBuilderForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aValidClaimRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDateAndChildrenDob;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndExistingClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anIdMatchedEligibilityNotConfirmedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedAddressNotMatchedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedFullAddressNotMatchedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedPostcodeNotMatchedResponse;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final String WEB_UI_VERSION = "1.1.1";

    @InjectMocks
    ClaimService claimService;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    EligibilityAndEntitlementService eligibilityAndEntitlementService;

    @Mock
    EventAuditor eventAuditor;

    @Mock
    ClaimMessageSender claimMessageSender;

    private final Map<String, Object> deviceFingerprint = DEVICE_FINGERPRINT;
    private final String deviceFingerprintHash = DigestUtils.md5Hex(DEVICE_FINGERPRINT.toString());

    @Test
    void shouldSaveNonExistingEligibleClaimantAndSendMessages() {
        //given
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertEligibleClaimResult(decision.getIdentityAndEligibilityResponse(), result);

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with claim status set to {1} when eligibility status is {0}")
    @CsvSource({
            "PENDING, PENDING",
            "NO_MATCH, REJECTED",
            "ERROR, ERROR",
            "INELIGIBLE, REJECTED"
    })
    void shouldSaveNonExistingIneligibleClaimant(EligibilityStatus eligibilityStatus, ClaimStatus claimStatus) {
        //given
        EligibilityAndEntitlementDecision eligibility = aDecisionWithStatus(eligibilityStatus);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(eligibility);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertIneligibleClaimResult(eligibilityStatus, claimStatus, eligibility.getIdentityAndEligibilityResponse(), result);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
    }

    @Test
    void shouldCorrectlyCalculateVoucherEntitlement() {
        //given
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(firstVoucherEntitlement));
        assertThat(result.getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    /**
     * Asserts that all eligibility statuses are mapped to a non null claim status.
     *
     * @param eligibilityStatus the eligibility status to test with
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with non null claim status for eligibility status {0}")
    @EnumSource(EligibilityStatus.class)
    void shouldSaveClaimantWithClaimStatus(EligibilityStatus eligibilityStatus) {
        //given
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(eligibilityStatus);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        verify(claimRepository).save(result.getClaim());
        assertThat(result.getClaim().getClaimStatus()).isNotNull();
        assertCorrectVerificationStatus(eligibilityStatus, result);
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        if (eligibilityStatus == ELIGIBLE) {
            verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
            verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
            verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        }
    }

    @Test
    void shouldRejectDuplicateClaim() {
        //given
        Claimant claimant = aClaimantWithLastName("New name");
        ClaimRequest request = aClaimRequestForClaimant(claimant);
        UUID existingClaimId = UUID.randomUUID();
        EligibilityAndEntitlementDecision decision = buildDuplicateDecisionWithExistingClaimId(existingClaimId);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        verify(claimRepository).save(result.getClaim());
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.REJECTED);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @Test
    void shouldReportClaimWhenTheClaimIsRejected() {
        //given
        // an INELIGIBLE response will cause a claim to be rejected
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(INELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        // then
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.REJECTED);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @Test
    void shouldHandleNullDeviceFingerprint() {
        //given
        Claimant claimant = aValidClaimant();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(aDecisionWithStatus(ELIGIBLE));
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .deviceFingerprint(null)
                .build();

        //when
        ClaimResult result = claimService.createClaim(claimRequest);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getDeviceFingerprint()).isNull();
        assertThat(result.getClaim().getDeviceFingerprintHash()).isNull();
    }

    @Test
    void shouldHandleEmptyDeviceFingerprint() {
        //given
        Claimant claimant = aValidClaimant();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(aDecisionWithStatus(ELIGIBLE));
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .deviceFingerprint(emptyMap())
                .build();

        //when
        ClaimResult result = claimService.createClaim(claimRequest);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getDeviceFingerprint()).isEmpty();
        assertThat(result.getClaim().getDeviceFingerprintHash()).isNull();
    }

    @Test
    void shouldHandleNullWebUIVersion() {
        //given
        Claimant claimant = aValidClaimant();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(aDecisionWithStatus(ELIGIBLE));
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .webUIVersion(null)
                .build();

        //when
        ClaimResult result = claimService.createClaim(claimRequest);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getWebUIVersion()).isNull();
    }

    @Test
    void shouldSaveNewClaimForMatchingNinoWhenIneligible() {
        //given
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(6);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        UUID existingClaimId = UUID.randomUUID();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndExistingClaim(INELIGIBLE, existingClaimId);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertIneligibleClaimResult(INELIGIBLE, ClaimStatus.REJECTED, decision.getIdentityAndEligibilityResponse(), result);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(newClaimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
    }

    /**
     * This is a false positive. PMD can't follow the data flow of `claimantDTO` inside the lambda.
     * https://github.com/pmd/pmd/issues/1304
     */
    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willThrow(TEST_EXCEPTION);
        ClaimRequest request = aValidClaimRequest();

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createClaim(request), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(TEST_EXCEPTION);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claimArgumentCaptor.capture());
        assertErrorClaimPersistedCorrectly(claimArgumentCaptor, request.getClaimant());
        ArgumentCaptor<FailureEvent> eventCaptor = ArgumentCaptor.forClass(FailureEvent.class);
        verify(eventAuditor).auditFailedEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(eventCaptor.getValue().getEventMetadata().get(FailureEvent.FAILED_EVENT_KEY)).isEqualTo(ClaimEventType.NEW_CLAIM);
        verifyNoInteractions(claimMessageSender);
    }

    @Test
    void shouldUpdateClaimWithCurrentIdentityAndEligibilityResponse() {
        // given
        Claim claim = aValidClaim();
        CombinedIdentityAndEligibilityResponse response = CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchFailedResponse();

        // when
        claimService.updateCurrentIdentityAndEligibilityResponse(claim, response);

        // then
        assertThat(claim.getCurrentIdentityAndEligibilityResponse()).isEqualTo(response);
        verify(claimRepository).save(claim);
    }

    @Test
    void shouldRejectClaimWhenNotPregnantAndNoChildrenMatch() {
        //given
        Claimant claimant = aClaimantWithExpectedDeliveryDateAndChildrenDob(null, SINGLE_SIX_MONTH_OLD);
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndChildren(ELIGIBLE, EligibilityOutcome.CONFIRMED, SINGLE_THREE_YEAR_OLD);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(result.getVerificationResult().getIsPregnantOrAtLeast1ChildMatched()).isFalse();
        assertThat(result.getVerificationResult().isAddressMismatch()).isFalse();

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.REJECTED);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForAddressMismatchResponses")
    void shouldRejectClaimWhenAddressMismatch(CombinedIdentityAndEligibilityResponse response) {
        //given
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndResponse(ELIGIBLE, response);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aValidClaimRequest();

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(result.getVerificationResult().isAddressMismatch()).isTrue();

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), response, ClaimAction.REJECTED);
        verify(claimMessageSender).sendDecisionPendingEmailMessage(result.getClaim());
        verify(claimMessageSender).sendUpdateYourAddressLetterMessage(result.getClaim());
        verifyNoMoreInteractions(claimMessageSender);
    }

    private void assertErrorClaimPersistedCorrectly(ArgumentCaptor<Claim> claimArgumentCaptor, Claimant claimant) {
        Claim actualClaim = claimArgumentCaptor.getValue();
        assertClaimPropertiesAreSet(actualClaim, ClaimStatus.ERROR, EligibilityStatus.ERROR, null);
        assertThat(actualClaim.getDwpHouseholdIdentifier()).isNull();
        assertThat(actualClaim.getHmrcHouseholdIdentifier()).isNull();
        assertThat(actualClaim.getClaimant()).isEqualTo(claimant);
    }

    private void assertEligibleClaimResult(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse, ClaimResult result) {
        assertThat(result).isNotNull();
        Claim claim = result.getClaim();
        assertClaimPropertiesAreSet(claim, ClaimStatus.NEW, ELIGIBLE, identityAndEligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(aVoucherEntitlementWithEntitlementDate(LocalDate.now())));
        assertThat(result.getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
    }

    private void assertIneligibleClaimResult(EligibilityStatus eligibilityStatus, ClaimStatus claimStatus,
                                             CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse, ClaimResult result) {
        assertThat(result).isNotNull();
        Claim actualClaim = result.getClaim();
        assertClaimPropertiesAreSet(actualClaim, claimStatus, eligibilityStatus, identityAndEligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.empty());
        assertThat(result.getVerificationResult()).isEqualTo(anIdMatchedEligibilityNotConfirmedVerificationResult());
    }

    private void assertClaimPropertiesAreSet(Claim claim, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus,
                                             CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        assertThat(claim).isNotNull();
        assertThat(claim.getClaimStatusTimestamp()).isNotNull();
        assertThat(claim.getEligibilityStatusTimestamp()).isNotNull();
        assertThat(claim.getClaimStatus()).isEqualTo(claimStatus);
        assertThat(claim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(claim.getInitialIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(claim.getCurrentIdentityAndEligibilityResponse()).isEqualTo(identityAndEligibilityResponse);
        assertThat(claim.getDeviceFingerprint()).isEqualTo(deviceFingerprint);
        assertThat(claim.getDeviceFingerprintHash()).isEqualTo(deviceFingerprintHash);
        assertThat(claim.getWebUIVersion()).isEqualTo(WEB_UI_VERSION);
    }

    private static Stream<Arguments> provideArgumentsForAddressMismatchResponses() {
        return Stream.of(
                Arguments.of(anIdMatchedEligibilityConfirmedPostcodeNotMatchedResponse()),
                Arguments.of(anIdMatchedEligibilityConfirmedAddressNotMatchedResponse()),
                Arguments.of(anIdMatchedEligibilityConfirmedFullAddressNotMatchedResponse())
        );
    }

    private void assertCorrectVerificationStatus(EligibilityStatus eligibilityStatus, ClaimResult result) {
        if (eligibilityStatus != DUPLICATE) {
            VerificationResult expectedVerificationResult = (eligibilityStatus == ELIGIBLE)
                    ? anAllMatchedVerificationResult() : anIdMatchedEligibilityNotConfirmedVerificationResult();
            assertThat(result.getVerificationResult()).isEqualTo(expectedVerificationResult);
        }
    }

}
