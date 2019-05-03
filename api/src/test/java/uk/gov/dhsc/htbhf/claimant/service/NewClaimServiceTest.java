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
import uk.gov.dhsc.htbhf.claimant.entitlement.EntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponseWithStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessagePayloadTestDataFactory.aValidNewCardRequestMessagePayloadWithClaimId;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class NewClaimServiceTest {

    @InjectMocks
    NewClaimService newClaimService;

    @Mock
    ClaimRepository claimRepository;

    @Mock
    EligibilityClient client;

    @Mock
    EligibilityStatusCalculator eligibilityStatusCalculator;

    @Mock
    EntitlementCalculator entitlementCalculator;

    @Mock
    ClaimAuditor claimAuditor;

    @Mock
    MessageQueueDAO messageQueueDAO;

    @Test
    void shouldSaveNonExistingEligibleClaimantAndSendNewCardMessage() {
        //given
        Claimant claimant = aValidClaimant();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(EligibilityStatus.ELIGIBLE);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isNotNull();
        assertThat(result.getClaim().getClaimStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(result.getClaim().getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(voucherEntitlement));

        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
        verify(claimRepository).save(result.getClaim());
        verifyNoMoreInteractions(claimRepository);
        verify(client).checkEligibility(claimant);
        verify(claimAuditor).auditNewClaim(result.getClaim());
        verifyCreateNewCardMessageSent(result);
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
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(eligibilityStatus);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);

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

        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
        verify(claimRepository).save(actualClaim);
        verifyNoMoreInteractions(claimRepository);
        verify(client).checkEligibility(claimant);
        verify(claimAuditor).auditNewClaim(actualClaim);
        verifyZeroInteractions(messageQueueDAO);
    }

    @Test
    void shouldCorrectlyCalculateVoucherEntitlement() {
        //given
        Claimant claimant = aValidClaimant();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponse();
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(EligibilityStatus.ELIGIBLE);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getVoucherEntitlement()).isEqualTo(Optional.of(voucherEntitlement));

        verify(entitlementCalculator).calculateVoucherEntitlement(
                Optional.ofNullable(claimant.getExpectedDeliveryDate()), getDateOfBirthOfChildren(eligibilityResponse), LocalDate.now());
        verify(claimAuditor).auditNewClaim(result.getClaim());
        verify(client).checkEligibility(claimant);
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
        verifyCreateNewCardMessageSent(result);
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
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(false);
        EligibilityResponse eligibilityResponse = anEligibilityResponseWithStatus(eligibilityStatus);
        given(client.checkEligibility(any())).willReturn(eligibilityResponse);
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        if (eligibilityStatus == EligibilityStatus.ELIGIBLE) {
            given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        }

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(claimRepository).save(result.getClaim());
        assertThat(result.getClaim().getClaimStatus()).isNotNull();
        verify(claimAuditor).auditNewClaim(result.getClaim());
        verify(client).checkEligibility(claimant);
        verify(eligibilityStatusCalculator).determineEligibilityStatus(eligibilityResponse);
        if (eligibilityStatus == EligibilityStatus.ELIGIBLE) {
            verify(entitlementCalculator).calculateVoucherEntitlement(
                    Optional.ofNullable(claimant.getExpectedDeliveryDate()), getDateOfBirthOfChildren(eligibilityResponse), LocalDate.now());
            verifyCreateNewCardMessageSent(result);
        }
    }

    @Test
    void shouldSaveDuplicateClaimantForMatchingNino() {
        //given
        Claimant claimant = aValidClaimant();
        given(claimRepository.liveClaimExistsForNino(any())).willReturn(true);

        //when
        ClaimResult result = newClaimService.createClaim(claimant);

        //then
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verify(claimRepository).save(result.getClaim());
        verifyNoMoreInteractions(claimRepository);
        verifyZeroInteractions(client, entitlementCalculator);
        verify(claimAuditor).auditNewClaim(result.getClaim());
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
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> newClaimService.createClaim(claimant), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        verify(claimRepository).save(any(Claim.class));
        verify(client).checkEligibility(claimant);
        verify(claimRepository).liveClaimExistsForNino(claimant.getNino());
        verifyNoMoreInteractions(claimRepository);
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimAuditor).auditNewClaim(claimArgumentCaptor.capture());
        assertThat(claimArgumentCaptor.getAllValues()).hasSize(1);
        assertClaimCorrectForAudit(claimArgumentCaptor, claimant);
        verifyZeroInteractions(messageQueueDAO);
    }

    private List<LocalDate> getDateOfBirthOfChildren(EligibilityResponse eligibilityResponse) {
        return eligibilityResponse.getChildren()
                .stream()
                .map(ChildDTO::getDateOfBirth)
                .collect(toList());
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

    private void verifyCreateNewCardMessageSent(ClaimResult result) {
        verify(messageQueueDAO).sendMessage(aValidNewCardRequestMessagePayloadWithClaimId(result.getClaim().getId()), CREATE_NEW_CARD);
    }
}
