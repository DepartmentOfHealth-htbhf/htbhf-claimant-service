package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_POSTCODE;

@ExtendWith(MockitoExtension.class)
class MIReporterTest {

    @Mock
    private PostcodeDataClient postcodeDataClient;
    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private MIReporter miReporter;

    @Test
    void shouldGetPostcodeDataAndSaveToClaim() {
        Claim claim = aClaimWithPostcodeData(null);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        given(postcodeDataClient.getPostcodeData(any())).willReturn(postcodeData);

        miReporter.reportClaim(claim);

        assertThat(claim.getPostcodeData()).isEqualTo(postcodeData);
        verify(postcodeDataClient).getPostcodeData(claim);
        verify(claimRepository).save(claim);
    }

    @Test
    void shouldNotGetPostcodeDataOrUpdateClaimWhenPostcodeDataExists() {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(VALID_POSTCODE);
        Claim claim = aClaimWithPostcodeData(postcodeData);

        miReporter.reportClaim(claim);

        verifyZeroInteractions(postcodeDataClient, claimRepository);
    }
}
