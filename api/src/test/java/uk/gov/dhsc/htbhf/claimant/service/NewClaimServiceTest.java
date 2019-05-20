package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.aDecisionWithStatusAndEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.DUPLICATE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@ExtendWith(MockitoExtension.class)
class NewClaimServiceTest {

    @InjectMocks
    NewClaimService newClaimService;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    EligibilityAndEntitlementService eligibilityAndEntitlementService;

    @Mock
    EventAuditor eventAuditor;

    @Mock
    MessageQueueDAO messageQueueDAO;

    @Test
    void shouldSaveNonExistingEligibleClaimantAndSendNewCardMessage() {
        //given
        Claimant claimant = aValidClaimant();
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        VoucherEntitlement secondVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(1));
        var entitlement = new PaymentCycleVoucherEntitlement(asList(firstVoucherEntitlement, secondVoucherEntitlement));
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndEntitlement(ELIGIBLE, entitlement);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(result.getClaim().getEligibilityStatus()).isEqualTo(ELIGIBLE);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(firstVoucherEntitlement));

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verifyCreateNewCardMessageSent(result, entitlement, decision.getDateOfBirthOfChildren());
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

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        Claim actualClaim = result.getClaim();
        assertThat(actualClaim).isNotNull();
        assertThat(actualClaim.getClaimStatus()).isEqualTo(claimStatus);
        assertThat(actualClaim.getClaimStatusTimestamp()).isNotNull();
        assertThat(actualClaim.getEligibilityStatus()).isEqualTo(eligibilityStatus);
        assertThat(actualClaim.getEligibilityStatusTimestamp()).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.empty());

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(actualClaim);
        verify(eventAuditor).auditNewClaim(actualClaim);
        verifyZeroInteractions(messageQueueDAO);
    }

    @Test
    void shouldCorrectlyCalculateVoucherEntitlement() {
        //given
        Claimant claimant = aValidClaimant();
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        VoucherEntitlement secondVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(1));
        var entitlement = new PaymentCycleVoucherEntitlement(asList(firstVoucherEntitlement, secondVoucherEntitlement));
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndEntitlement(ELIGIBLE, entitlement);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(firstVoucherEntitlement));

        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verifyCreateNewCardMessageSent(result, entitlement, decision.getDateOfBirthOfChildren());
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
        VoucherEntitlement firstVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now());
        VoucherEntitlement secondVoucherEntitlement = aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(1));
        var entitlement = new PaymentCycleVoucherEntitlement(asList(firstVoucherEntitlement, secondVoucherEntitlement));
        EligibilityAndEntitlementDecision decision = aDecisionWithStatusAndEntitlement(eligibilityStatus, entitlement);
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willReturn(decision);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(claimRepository).save(result.getClaim());
        assertThat(result.getClaim().getClaimStatus()).isNotNull();
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        if (eligibilityStatus == ELIGIBLE) {
            verifyCreateNewCardMessageSent(result, entitlement, decision.getDateOfBirthOfChildren());
        }
    }

    @Test
    void shouldSaveDuplicateClaimantForMatchingNino() {
        //given
        Claimant claimant = aValidClaimant();
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any()))
                .willReturn(EligibilityAndEntitlementDecision.buildWithStatus(DUPLICATE));

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(result.getClaim());
        verify(eventAuditor).auditNewClaim(result.getClaim());
        verifyZeroInteractions(messageQueueDAO);
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
        RuntimeException testException = new RuntimeException("Test exception");
        given(eligibilityAndEntitlementService.evaluateNewClaimant(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> newClaimService.createClaim(claimant), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        verify(eligibilityAndEntitlementService).evaluateNewClaimant(claimant);
        verify(claimRepository).save(any(Claim.class));
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(eventAuditor).auditNewClaim(claimArgumentCaptor.capture());
        assertThat(claimArgumentCaptor.getAllValues()).hasSize(1);
        assertClaimCorrectForAudit(claimArgumentCaptor, claimant);
        verifyZeroInteractions(messageQueueDAO);
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
    }

    private void verifyCreateNewCardMessageSent(ClaimResult result, PaymentCycleVoucherEntitlement voucherEntitlement, List<LocalDate> datesOfBirth) {
        NewCardRequestMessagePayload newCardRequestMessagePayload = NewCardRequestMessagePayload.builder()
                .claimId(result.getClaim().getId())
                .voucherEntitlement(voucherEntitlement)
                .datesOfBirthOfChildren(datesOfBirth)
                .build();
        verify(messageQueueDAO).sendMessage(newCardRequestMessagePayload, CREATE_NEW_CARD);
    }
}
