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
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
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
import uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.APPLICATION_SUCCESS_CHILDREN_MISMATCH;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.UPDATE_YOUR_ADDRESS;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildDuplicateDecisionWithExistingClaimId;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestBuilderForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aValidClaimRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndExistingClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.DEVICE_FINGERPRINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResultWithPhoneAndEmail;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anIdMatchedEligibilityNotConfirmedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.MATCHED;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_HELD;
import static uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome.NOT_MATCHED;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final List<LocalDate> NULL_CHILDREN = null;
    private static final List<LocalDate> NO_CHILDREN = emptyList();

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

    @ParameterizedTest(name = "Initially declared children dobs: {0}, eligibility response children: {1}")
    @MethodSource("provideAllChildrenRegisteredChildrenDobs")
    void shouldSaveNewEligibleClaimantAndSendMessagesAllDeclaredChildrenPresentInEligibilityResponse(List<LocalDate> initiallyDeclaredChildren,
                                                                                                     List<LocalDate> eligibilityResponseChildren) {
        //given
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse =
                anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(eligibilityResponseChildren);
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndResponse(ELIGIBLE, identityAndEligibilityResponse);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        Claimant claimant = aClaimantWithChildrenDob(initiallyDeclaredChildren);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertEligibleClaimResult(decision.getIdentityAndEligibilityResponse(), result);

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmail(result.getClaim(), decision, EmailType.INSTANT_SUCCESS);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @Test
    void shouldSaveNewEligibleClaimantThatIsPregnantWithNoChildren() {
        //given
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(NO_CHILDREN);
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndResponse(ELIGIBLE, identityAndEligibilityResponse);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        Claimant pregnantOnlyClaimant = aClaimantWithChildrenDob(NULL_CHILDREN);
        ClaimRequest request = aClaimRequestForClaimant(pregnantOnlyClaimant);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertEligibleClaimResult(decision.getIdentityAndEligibilityResponse(), result);

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(pregnantOnlyClaimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmail(result.getClaim(), decision, EmailType.INSTANT_SUCCESS);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    @ParameterizedTest(name = "Initially declared children dobs: {0}, eligibility response children: {1}")
    @MethodSource("providePartialMatchChildrenDobs")
    void shouldSaveNewEligibleClaimantAndSendMessagesWhenPartialChildrenMatch(List<LocalDate> initiallyDeclaredChildren,
                                                                              List<LocalDate> eligibilityResponseChildren) {
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse =
                anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(eligibilityResponseChildren);
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndResponse(ELIGIBLE, identityAndEligibilityResponse);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        Claimant claimant = aClaimantWithChildrenDob(initiallyDeclaredChildren);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        assertEligibleClaimResult(decision.getIdentityAndEligibilityResponse(), result);

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmail(result.getClaim(), decision, EmailType.INSTANT_SUCCESS_PARTIAL_CHILDREN_MATCH);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    private static Stream<Arguments> providePartialMatchChildrenDobs() {
        //First param is initiallyDeclaredChildren, second param is those returned from the eligibility service.
        return Stream.of(
                Arguments.of(singletonList(LISA_DATE_OF_BIRTH), NULL_CHILDREN),
                Arguments.of(singletonList(LISA_DATE_OF_BIRTH), NO_CHILDREN),
                Arguments.of(List.of(MAGGIE_DATE_OF_BIRTH, LISA_DATE_OF_BIRTH, BART_DATE_OF_BIRTH), MAGGIE_AND_LISA_DOBS)
        );
    }

    private static Stream<Arguments> provideAllChildrenRegisteredChildrenDobs() {
        //First param is initiallyDeclaredChildren, second param is those returned from the eligibility service.
        return Stream.of(
                Arguments.of(singletonList(MAGGIE_DATE_OF_BIRTH), MAGGIE_AND_LISA_DOBS),
                Arguments.of(singletonList(LISA_DATE_OF_BIRTH), MAGGIE_AND_LISA_DOBS),
                Arguments.of(NULL_CHILDREN, singletonList(LISA_DATE_OF_BIRTH)),
                Arguments.of(NO_CHILDREN, singletonList(LISA_DATE_OF_BIRTH))
        );
    }

    @ParameterizedTest
    @MethodSource("emailOrPhoneMismatchArguments")
    void shouldSaveNewEligibleClaimantAndSendWeWillLetYouKnowEmailAndInstantSuccessLetterWhenEmailOrPhoneMismatch(VerificationOutcome emailVerification,
                                                                                                                  VerificationOutcome phoneVerification,
                                                                                                                  List<LocalDate> declaredChildrenDob,
                                                                                                                  List<LocalDate> benefitAgencyChildrenDob,
                                                                                                                  LetterType letterType) {
        //given
        ClaimRequest request = aClaimRequestForClaimant(aClaimantWithChildrenDob(declaredChildrenDob));
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndResponse(ELIGIBLE, anIdMatchedEligibilityConfirmedUCResponseWithMatches(
                phoneVerification, emailVerification, benefitAgencyChildrenDob));
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);

        //when
        ClaimResult result = claimService.createClaim(request);

        //then
        VerificationResult expectedVerificationResult = anAllMatchedVerificationResultWithPhoneAndEmail(phoneVerification, emailVerification);
        assertEligibleClaimResult(decision.getIdentityAndEligibilityResponse(), result, expectedVerificationResult);

        Claim claim = result.getClaim();
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(request.getClaimant());
        verify(claimRepository).save(claim);
        verify(eventAuditor).auditNewClaim(claim);
        verify(claimMessageSender).sendDecisionPendingEmailMessage(claim);
        verify(claimMessageSender).sendLetterWithAddressAndPaymentFieldsMessage(claim, decision, letterType);
        verify(claimMessageSender).sendNewCardMessage(claim, decision);
        verify(claimMessageSender).sendReportClaimMessage(claim, decision.getIdentityAndEligibilityResponse(), ClaimAction.NEW);
        verifyNoMoreInteractions(claimMessageSender);
    }

    // children match when the children return from the benefit agency contains all of the declared children.
    private static Stream<Arguments> emailOrPhoneMismatchArguments() {
        return Stream.of(
                // email match, phone match, declared children dob, benefit agency dob, letter type
                Arguments.of(NOT_MATCHED, MATCHED, MAGGIE_AND_LISA_DOBS, MAGGIE_AND_LISA_DOBS, APPLICATION_SUCCESS_CHILDREN_MATCH),
                Arguments.of(NOT_HELD, MATCHED, MAGGIE_AND_LISA_DOBS, List.of(MAGGIE_DATE_OF_BIRTH), APPLICATION_SUCCESS_CHILDREN_MISMATCH),
                Arguments.of(MATCHED, NOT_MATCHED, MAGGIE_AND_LISA_DOBS, List.of(MAGGIE_DATE_OF_BIRTH), APPLICATION_SUCCESS_CHILDREN_MISMATCH),
                Arguments.of(NOT_MATCHED, NOT_MATCHED, MAGGIE_AND_LISA_DOBS, List.of(MAGGIE_DATE_OF_BIRTH), APPLICATION_SUCCESS_CHILDREN_MISMATCH),
                Arguments.of(NOT_MATCHED, NOT_MATCHED, MAGGIE_AND_LISA_DOBS, emptyList(), APPLICATION_SUCCESS_CHILDREN_MISMATCH),
                Arguments.of(NOT_MATCHED, NOT_MATCHED, emptyList(), MAGGIE_AND_LISA_DOBS, APPLICATION_SUCCESS_CHILDREN_MATCH),
                Arguments.of(NOT_MATCHED, NOT_MATCHED, List.of(MAGGIE_DATE_OF_BIRTH), MAGGIE_AND_LISA_DOBS, APPLICATION_SUCCESS_CHILDREN_MATCH)
        );
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with claim status set to {1} when eligibility status is {0}")
    @CsvSource({
            "PENDING, PENDING",
            "NO_MATCH, REJECTED",
            "ERROR, ERROR",
            "INELIGIBLE, REJECTED"
    })
    void shouldSaveNewIneligibleClaimant(EligibilityStatus eligibilityStatus, ClaimStatus claimStatus) {
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
        verify(claimMessageSender).sendInstantSuccessEmail(result.getClaim(), decision, EmailType.INSTANT_SUCCESS);
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
            verify(claimMessageSender).sendInstantSuccessEmail(result.getClaim(), decision, EmailType.INSTANT_SUCCESS);
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
        CombinedIdentityAndEligibilityResponse response = anIdMatchFailedResponse();

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
        verify(claimMessageSender).sendLetterWithAddressOnlyMessage(result.getClaim(), UPDATE_YOUR_ADDRESS);
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
        VerificationResult expectedVerificationResult = anAllMatchedVerificationResult();
        assertEligibleClaimResult(identityAndEligibilityResponse, result, expectedVerificationResult);
    }

    private void assertEligibleClaimResult(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                           ClaimResult result,
                                           VerificationResult expectedVerificationResult) {
        assertThat(result).isNotNull();
        Claim claim = result.getClaim();
        assertClaimPropertiesAreSet(claim, ClaimStatus.NEW, ELIGIBLE, identityAndEligibilityResponse);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(aVoucherEntitlementWithEntitlementDate(LocalDate.now())));
        assertThat(result.getVerificationResult()).isEqualTo(expectedVerificationResult);
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
