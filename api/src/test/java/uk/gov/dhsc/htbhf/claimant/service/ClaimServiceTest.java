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
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;

@ExtendWith(MockitoExtension.class)
public class ClaimServiceTest {

    @InjectMocks
    ClaimService claimService;

    @Mock
    ClaimantRepository claimantRepository;

    @Mock
    EligibilityClient client;

    @Mock
    ClaimDTOToClaimConverter converter;

    @Mock
    EligibilityStatusCalculator eligibilityStatusCalculator;

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @ParameterizedTest(name = "Should save claimant with claim status set to {1} when eligibility status is {0}")
    @CsvSource({
            "ELIGIBLE, NEW",
            "PENDING, PENDING",
            "NO_MATCH, REJECTED",
            "ERROR, ERROR",
            "INELIGIBLE, REJECTED"
    })
    public void shouldSaveNonExistingClaimant(EligibilityStatus eligibilityStatus, ClaimStatus claimStatus) {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = buildClaim(claimant);
        ClaimDTO claimDTO = aValidClaimDTO();
        given(converter.convert(any())).willReturn(claim);
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);

        //when
        claimService.createClaim(claimDTO);

        //then
        Claimant expectedClaimant = buildExpectedClaimant(claimant, claimStatus, eligibilityStatus);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(eligibilityStatusCalculator).determineEligibilityStatus(anEligibilityResponse());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verify(client).checkEligibility(claimant);
        verify(converter).convert(claimDTO);
    }

    /**
     * Asserts that all eligibility statuses are mapped to a non null claim status.
     * @param eligibilityStatus the eligibility status to test with
     */
    @ParameterizedTest(name = "Should save claimant with non null claim status for all eligibility statuses")
    @EnumSource(EligibilityStatus.class)
    public void shouldSaveClaimantWithClaimStatus(EligibilityStatus eligibilityStatus) {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = buildClaim(claimant);
        ClaimDTO claimDTO = aValidClaimDTO();
        given(converter.convert(any())).willReturn(claim);
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(false);
        given(client.checkEligibility(any())).willReturn(anEligibilityResponse());
        given(eligibilityStatusCalculator.determineEligibilityStatus(any())).willReturn(eligibilityStatus);

        //when
        claimService.createClaim(claimDTO);

        //then
        ArgumentCaptor<Claimant> argumentCaptor = ArgumentCaptor.forClass(Claimant.class);
        verify(claimantRepository).save(argumentCaptor.capture());
        Claimant savedClaimant = argumentCaptor.getValue();
        assertThat(savedClaimant.getClaimStatus()).isNotNull();
    }

    @Test
    void shouldSaveDuplicateClaimantForMatchingNino() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = buildClaim(claimant);
        ClaimDTO claimDTO = aValidClaimDTO();
        given(converter.convert(any())).willReturn(claim);
        given(claimantRepository.eligibleClaimExistsForNino(any())).willReturn(true);

        //when
        claimService.createClaim(claimDTO);

        //then
        Claimant expectedClaimant = buildExpectedClaimant(claimant, ClaimStatus.REJECTED, EligibilityStatus.DUPLICATE);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verify(claimantRepository).save(expectedClaimant);
        verifyNoMoreInteractions(claimantRepository);
        verifyZeroInteractions(client);
        verify(converter).convert(claimDTO);
    }

    /**
     * This is a false positive. PMD can't follow the data flow of `claimant` inside the lambda.
     * https://github.com/pmd/pmd/issues/1304
     */
    @Test
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    void shouldSaveClaimantWhenEligibilityThrowsException() {
        //given
        Claimant claimant = aValidClaimantBuilder().build();
        Claim claim = buildClaim(claimant);
        ClaimDTO claimDTO = aValidClaimDTO();
        given(converter.convert(any())).willReturn(claim);
        RuntimeException testException = new RuntimeException("Test exception");
        given(client.checkEligibility(any())).willThrow(testException);

        //when
        RuntimeException thrown = catchThrowableOfType(() -> claimService.createClaim(claimDTO), RuntimeException.class);

        //then
        assertThat(thrown).isEqualTo(testException);
        Claimant expectedClaimant = buildExpectedClaimant(claimant, ClaimStatus.ERROR, EligibilityStatus.ERROR);
        verify(claimantRepository).save(expectedClaimant);
        verify(client).checkEligibility(claimant);
        verify(claimantRepository).eligibleClaimExistsForNino(claimant.getNino());
        verifyNoMoreInteractions(claimantRepository);
        verify(converter).convert(claimDTO);
    }

    private Claim buildClaim(Claimant claimant) {
        return Claim.builder().claimant(claimant).build();
    }

    private Claimant buildExpectedClaimant(Claimant claimant, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        return claimant.toBuilder()
                .claimStatus(claimStatus)
                .eligibilityStatus(eligibilityStatus).build();
    }
}
