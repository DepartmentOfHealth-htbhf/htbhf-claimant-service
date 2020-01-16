package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.model.NewClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTOWithEligibilityOverride;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

@ExtendWith(MockitoExtension.class)
class NewClaimDTOToClaimRequestConverterTest {

    @InjectMocks
    NewClaimDTOToClaimRequestConverter claimRequestConverter;

    @Mock
    ClaimantDTOToClaimantConverter claimantConverter;

    @Test
    void shouldConvertNewClaimDTOToClaimRequest() {
        Claimant claimant = aValidClaimant();
        given(claimantConverter.convert(any())).willReturn(claimant);
        NewClaimDTO dto = aValidClaimDTO();

        ClaimRequest claimRequest = claimRequestConverter.convert(dto);

        assertThat(claimRequest).isNotNull();
        assertThat(claimRequest.getClaimant()).isEqualTo(claimant);
        assertThat(claimRequest.getDeviceFingerprint()).isEqualTo(dto.getDeviceFingerprint());
        assertThat(claimRequest.getWebUIVersion()).isEqualTo(dto.getWebUIVersion());
        assertThat(dto.getEligibilityOverride()).isNull();
    }

    @Test
    void shouldConvertNewClaimDTOToClaimRequestWithEligibilityOverride() {
        Claimant claimant = aValidClaimant();
        given(claimantConverter.convert(any())).willReturn(claimant);
        NewClaimDTO dto = aValidClaimDTOWithEligibilityOverride(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS, NO_CHILDREN, EligibilityOutcome.CONFIRMED);

        ClaimRequest claimRequest = claimRequestConverter.convert(dto);

        assertThat(claimRequest).isNotNull();
        assertThat(claimRequest.getClaimant()).isEqualTo(claimant);
        assertThat(claimRequest.getDeviceFingerprint()).isEqualTo(dto.getDeviceFingerprint());
        assertThat(claimRequest.getWebUIVersion()).isEqualTo(dto.getWebUIVersion());
        EligibilityOverride eligibilityOverride = claimRequest.getEligibilityOverride();
        assertThat(eligibilityOverride).isNotNull();
        assertThat(eligibilityOverride.getEligibilityOutcome()).isEqualTo(EligibilityOutcome.CONFIRMED);
    }

}