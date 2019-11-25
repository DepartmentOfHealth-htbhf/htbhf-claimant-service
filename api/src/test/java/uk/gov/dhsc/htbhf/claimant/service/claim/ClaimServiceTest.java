package uk.gov.dhsc.htbhf.claimant.service.claim;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;

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
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.DEVICE_FINGERPRINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestBuilderForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aClaimRequestForClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.INELIGIBLE;

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
        Claimant claimant = aValidClaimant();
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(result.getClaim().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(result.getClaim().getDeviceFingerprint()).isEqualTo(deviceFingerprint);
        assertThat(result.getClaim().getDeviceFingerprintHash()).isEqualTo(deviceFingerprintHash);
        assertThat(result.getClaim().getWebUIVersion()).isEqualTo(WEB_UI_VERSION);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(firstVoucherEntitlement));

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getDateOfBirthOfChildren(), ClaimAction.NEW);
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
        Claimant claimant = aValidClaimant();
        EligibilityAndEntitlementDecision eligibility = aDecisionWithStatus(eligibilityStatus);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(eligibility);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result).isNotNull();
        Claim actualClaim = result.getClaim();
        assertThat(actualClaim).isNotNull();
        assertThat(actualClaim.getClaimStatus()).isEqualTo(claimStatus);
        assertThat(actualClaim.getClaimStatusTimestamp()).isNotNull();
        assertThat(actualClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(actualClaim.getEligibilityStatusTimestamp()).isNotNull();
        assertThat(actualClaim.getDeviceFingerprint()).isEqualTo(deviceFingerprint);
        assertThat(actualClaim.getDeviceFingerprintHash()).isEqualTo(deviceFingerprintHash);
        assertThat(actualClaim.getWebUIVersion()).isEqualTo(WEB_UI_VERSION);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.empty());

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(actualClaim);
        verify(eventAuditor).auditNewClaim(actualClaim);
    }

    @Test
    void shouldCorrectlyCalculateVoucherEntitlement() {
        //given
        Claimant claimant = aValidClaimant();
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(firstVoucherEntitlement));

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getDateOfBirthOfChildren(), ClaimAction.NEW);
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
        Claimant claimant = aValidClaimant();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(eligibilityStatus);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        verify(claimRepository).save(result.getClaim());
        assertThat(result.getClaim().getClaimStatus()).isNotNull();
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        if (eligibilityStatus == ELIGIBLE) {
            verify(claimMessageSender).sendInstantSuccessEmailMessage(result.getClaim(), decision);
            verify(claimMessageSender).sendNewCardMessage(result.getClaim(), decision);
            verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), decision.getDateOfBirthOfChildren(), ClaimAction.NEW);
        }
    }

    @Test
    void shouldUpdateClaimAndReturnUpdatedFieldsForMatchingNinoWhenEligible() {
        //given
        Claimant existingClaimant = aClaimantWithLastName("Old name");
        Claimant newClaimant = aClaimantWithLastName("New name");
        Claim existingClaim = aClaimWithClaimant(existingClaimant);
        UUID existingClaimId = existingClaim.getId();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        given(claimRepository.findClaim(any())).willReturn(existingClaim);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isTrue();
        assertThat(result.getUpdatedFields()).isEqualTo(singletonList(LAST_NAME.getFieldName()));
        assertThat(result.getClaim()).isEqualTo(existingClaim);
        assertThat(result.getClaim().getClaimant().getLastName()).isEqualTo(newClaimant.getLastName());
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(newClaimant);
        verify(claimRepository).findClaim(existingClaimId);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditUpdatedClaim(result.getClaim(), singletonList(LAST_NAME));
        verify(claimMessageSender).sendReportClaimMessageWithUpdatedClaimantFields(existingClaim, decision.getDateOfBirthOfChildren(), List.of(LAST_NAME));
        verifyNoMoreInteractions(claimMessageSender);
    }

    @Test
    void shouldUpdateClaimAndReturnClaimUpdatedForMatchingNinoWhenEligibleAndNoFieldsHaveChanged() {
        //given
        Claimant existingClaimant = aValidClaimant();
        Claimant newClaimant = aValidClaimant();
        Claim existingClaim = aClaimWithClaimant(existingClaimant);
        UUID existingClaimId = UUID.randomUUID();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        given(claimRepository.findClaim(any())).willReturn(existingClaim);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isTrue();
        assertThat(result.getUpdatedFields()).isEmpty();
        assertThat(result.getClaim()).isEqualTo(existingClaim);
        assertThat(result.getClaim().getClaimant().getExpectedDeliveryDate()).isEqualTo(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(newClaimant);
        verify(claimRepository).findClaim(existingClaimId);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditUpdatedClaim(result.getClaim(), emptyList());
        verify(claimMessageSender).sendReportClaimMessageWithUpdatedClaimantFields(existingClaim, decision.getDateOfBirthOfChildren(), emptyList());
        verifyNoMoreInteractions(claimMessageSender);
    }

    @Test
    void shouldSendAdditionalPregnancyPaymentMessageWhenDueDateProvided() {
        //given
        Claimant existingClaimant = aClaimantWithExpectedDeliveryDate(null);
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(6);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        shouldSendAdditionalPregnancyPaymentMessage(existingClaimant, newClaimant);
    }

    @Test
    void shouldSendAdditionalPregnancyPaymentMessageWhenExistingDueDateUpdated() {
        //given
        Claimant existingClaimant = aClaimantWithExpectedDeliveryDate(LocalDate.now());
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(1);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        shouldSendAdditionalPregnancyPaymentMessage(existingClaimant, newClaimant);
    }

    @Test
    void shouldReportClaimWhenTheClaimIsRejected() {
        //given
        Claimant claimant = aValidClaimant();
        // an INELIGIBLE response will cause a claim to be rejected
        EligibilityAndEntitlementDecision eligibility = aDecisionWithStatus(INELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(eligibility);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        // then
        verify(claimMessageSender).sendReportClaimMessage(result.getClaim(), eligibility.getDateOfBirthOfChildren(), ClaimAction.REJECTED);
        verifyNoMoreInteractions(claimMessageSender);
    }

    private void shouldSendAdditionalPregnancyPaymentMessage(Claimant existingClaimant, Claimant newClaimant) {
        Claim existingClaim = aClaimWithClaimant(existingClaimant);
        UUID existingClaimId = existingClaim.getId();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build());
        given(claimRepository.findClaim(any())).willReturn(existingClaim);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isTrue();
        assertThat(result.getUpdatedFields()).containsExactly(EXPECTED_DELIVERY_DATE.getFieldName());
        verify(claimMessageSender).sendAdditionalPaymentMessage(existingClaim);
    }

    @Test
    void shouldNotSendAdditionalPregnancyPaymentMessageWhenDueDateSetToNull() {
        //given
        Claimant existingClaimant = aClaimantWithExpectedDeliveryDate(LocalDate.now());
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(null);
        Claim existingClaim = aClaimWithClaimant(existingClaimant);
        UUID existingClaimId = existingClaim.getId();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        given(claimRepository.findClaim(any())).willReturn(existingClaim);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isTrue();
        assertThat(result.getUpdatedFields()).isEqualTo(singletonList(EXPECTED_DELIVERY_DATE.getFieldName()));
    }

    @Test
    void shouldNotUpdateDeviceFingerprintWhenUpdatingAClaim() {
        //given
        Claimant existingClaimant = aClaimantWithExpectedDeliveryDate(null);
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(6);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        Claim existingClaim = aClaimWithClaimant(existingClaimant);
        UUID existingClaimId = UUID.randomUUID();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build());
        given(claimRepository.findClaim(any())).willReturn(existingClaim);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isTrue();
        assertThat(result.getClaim().getDeviceFingerprint()).isNotEqualTo(deviceFingerprint);
        assertThat(result.getClaim().getDeviceFingerprintHash()).isNotEqualTo(deviceFingerprintHash);
    }

    @Test
    void shouldHandleNullDeviceFingerprint() {
        //given
        Claimant claimant = aValidClaimant();
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .deviceFingerprint(null)
                .build();

        //when
        ClaimResult result = claimService.createOrUpdateClaim(claimRequest);

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
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .deviceFingerprint(emptyMap())
                .build();

        //when
        ClaimResult result = claimService.createOrUpdateClaim(claimRequest);

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
        EligibilityAndEntitlementDecision decision = aDecisionWithStatus(ELIGIBLE);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest claimRequest = aClaimRequestBuilderForClaimant(claimant)
                .webUIVersion(null)
                .build();

        //when
        ClaimResult result = claimService.createOrUpdateClaim(claimRequest);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getWebUIVersion()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenExistingClaimNotFound() {
        //given
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(6);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        UUID existingClaimId = UUID.randomUUID();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build());
        EntityNotFoundException expectedException = new EntityNotFoundException("Not found");
        given(claimRepository.findClaim(any())).willThrow(expectedException);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        EntityNotFoundException exception = catchThrowableOfType(
                () -> claimService.createOrUpdateClaim(request), EntityNotFoundException.class);

        //then
        assertThat(exception).isEqualTo(expectedException);
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claimArgumentCaptor.capture());
        assertClaimCorrectForAudit(claimArgumentCaptor, newClaimant);
        ArgumentCaptor<FailureEvent> eventCaptor = ArgumentCaptor.forClass(FailureEvent.class);
        verify(eventAuditor).auditFailedEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(eventCaptor.getValue().getEventMetadata().get(FailureEvent.FAILED_EVENT_KEY)).isEqualTo(ClaimEventType.NEW_CLAIM);
    }

    @Test
    void shouldSaveNewClaimForMatchingNinoWhenIneligible() {
        //given
        LocalDate expectedDeliveryDate = LocalDate.now().plusMonths(6);
        Claimant newClaimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        UUID existingClaimId = UUID.randomUUID();
        EligibilityAndEntitlementDecision decision = EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(INELIGIBLE)
                .existingClaimId(existingClaimId)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);
        ClaimRequest request = aClaimRequestForClaimant(newClaimant);

        //when
        ClaimResult result = claimService.createOrUpdateClaim(request);

        //then
        assertThat(result.getClaimUpdated()).isNull();
        assertThat(result.getUpdatedFields()).isNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getEligibilityStatus()).isEqualTo(INELIGIBLE);
        assertThat(result.getClaim().getDeviceFingerprint()).isEqualTo(deviceFingerprint);
        assertThat(result.getClaim().getDeviceFingerprintHash()).isEqualTo(deviceFingerprintHash);
        assertThat(result.getClaim().getWebUIVersion()).isEqualTo(WEB_UI_VERSION);
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
        Claimant claimant = aValidClaimant();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willThrow(TEST_EXCEPTION);
        ClaimRequest request = aClaimRequestForClaimant(claimant);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createOrUpdateClaim(request), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(TEST_EXCEPTION);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claimArgumentCaptor.capture());
        assertClaimCorrectForAudit(claimArgumentCaptor, claimant);
        ArgumentCaptor<FailureEvent> eventCaptor = ArgumentCaptor.forClass(FailureEvent.class);
        verify(eventAuditor).auditFailedEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(eventCaptor.getValue().getEventMetadata().get(FailureEvent.FAILED_EVENT_KEY)).isEqualTo(ClaimEventType.NEW_CLAIM);
        verifyNoInteractions(claimMessageSender);
    }

    private void assertClaimCorrectForAudit(ArgumentCaptor<Claim> claimArgumentCaptor, Claimant claimant) {
        Claim actualClaim = claimArgumentCaptor.getValue();
        assertThat(actualClaim.getDwpHouseholdIdentifier()).isNull();
        assertThat(actualClaim.getHmrcHouseholdIdentifier()).isNull();
        assertThat(actualClaim.getClaimStatusTimestamp()).isNotNull();
        assertThat(actualClaim.getClaimStatus()).isEqualTo(ClaimStatus.ERROR);
        assertThat(actualClaim.getEligibilityStatus()).isEqualTo(EligibilityStatus.ERROR);
        assertThat(actualClaim.getEligibilityStatusTimestamp()).isNotNull();
        assertThat(actualClaim.getClaimant()).isEqualTo(claimant);
        assertThat(actualClaim.getDeviceFingerprint()).isEqualTo(deviceFingerprint);
        assertThat(actualClaim.getDeviceFingerprintHash()).isEqualTo(deviceFingerprintHash);
        assertThat(actualClaim.getWebUIVersion()).isEqualTo(WEB_UI_VERSION);
    }
}
