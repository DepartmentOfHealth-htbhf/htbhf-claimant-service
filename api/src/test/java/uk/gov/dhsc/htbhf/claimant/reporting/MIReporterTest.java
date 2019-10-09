package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.PostcodesClientException;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_POSTCODE;

@ExtendWith(MockitoExtension.class)
class MIReporterTest {

    private static final String POSTCODES_IO_BASE_URI = "http://localhost:8120/";
    private static final String POSTCODES_IO_PATH = "/postcodes/";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ClaimRepository claimRepository;

    private MIReporter miReporter;

    @BeforeEach
    public void init() {
        miReporter = new MIReporter(POSTCODES_IO_BASE_URI, restTemplate, claimRepository);
    }

    @Test
    void shouldGetPostcodeDataAndSaveToClaim() {
        Claim claim = aValidClaimBuilder().postcodeData(null).build();
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeDataResponse response = createPostcodeData(postcode);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willReturn(response);

        miReporter.reportClaim(claim);

        assertThat(claim.getPostcodeData()).isEqualTo(response.getPostcodeData());
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
        verify(claimRepository).save(claim);
    }

    @Test
    void shouldNotGetPostcodeDataOrUpdateClaimWhenPostcodeDataExists() {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(VALID_POSTCODE);
        Claim claim = aValidClaimBuilder().postcodeData(postcodeData).build();

        miReporter.reportClaim(claim);

        verifyZeroInteractions(restTemplate);
        verifyZeroInteractions(claimRepository);
    }

    @Test
    void shouldSaveNotFoundPostcodeDataWhenPostcodeNotFound() {
        Claim claim = aValidClaimBuilder().postcodeData(null).build();
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willThrow(new HttpClientErrorException(NOT_FOUND));

        miReporter.reportClaim(claim);

        assertThat(claim.getPostcodeData()).isEqualTo(PostcodeData.NOT_FOUND);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
        verify(claimRepository).save(claim);
    }

    @Test
    void shouldRethrowErrorWhenUnableToCallPostcodesIo() {
        Claim claim = aValidClaimBuilder().postcodeData(null).build();
        String postcode = claim.getClaimant().getAddress().getPostcode();
        HttpClientErrorException expectedException = new HttpClientErrorException(INTERNAL_SERVER_ERROR);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willThrow(expectedException);

        HttpClientErrorException actualException = catchThrowableOfType(() -> miReporter.reportClaim(claim), HttpClientErrorException.class);

        assertThat(actualException).isEqualTo(expectedException);
        assertThat(claim.getPostcodeData()).isNull();

        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
        verifyZeroInteractions(claimRepository);
    }

    @Test
    void shouldThrowExceptionIfPostcodeDataResponseIsNull() {
        Claim claim = aValidClaimBuilder().postcodeData(null).build();
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeDataResponse response = new PostcodeDataResponse(null);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willReturn(response);

        PostcodesClientException exception = catchThrowableOfType(() -> miReporter.reportClaim(claim), PostcodesClientException.class);

        assertThat(exception).isNotNull();
        assertThat(claim.getPostcodeData()).isNull();
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
        verifyZeroInteractions(claimRepository);
    }

    private String getExpectedPostcodeUrl(String postcode) {
        String postcodeWithoutSpace = postcode.replace(" ", "");
        return POSTCODES_IO_BASE_URI + POSTCODES_IO_PATH + postcodeWithoutSpace;
    }

    private PostcodeDataResponse createPostcodeData(String postcode) {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        return new PostcodeDataResponse(postcodeData);
    }
}
