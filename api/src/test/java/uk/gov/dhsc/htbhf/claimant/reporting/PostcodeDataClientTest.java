package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.exception.PostcodesIoClientException;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;

@ExtendWith(MockitoExtension.class)
class PostcodeDataClientTest {

    private static final String POSTCODES_IO_BASE_URI = "http://localhost:8120";
    private static final String POSTCODES_IO_PATH = "/postcodes/";

    @Mock
    private RestTemplate restTemplate;

    private PostcodeDataClient postcodeDataClient;

    @BeforeEach
    public void init() {
        postcodeDataClient = new PostcodeDataClient(POSTCODES_IO_BASE_URI, restTemplate);
    }

    @Test
    void shouldGetPostcodeData() {
        Claim claim = aClaimWithPostcodeData(null);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeDataResponse response = createPostcodeData(postcode);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willReturn(response);

        PostcodeData postcodeData = postcodeDataClient.getPostcodeData(claim);

        assertThat(postcodeData).isEqualTo(response.getPostcodeData());
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
    }

    @Test
    void shouldReturnNotFoundPostcodeDataWhenPostcodeNotFound() {
        Claim claim = aClaimWithPostcodeData(null);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willThrow(new HttpClientErrorException(NOT_FOUND));

        PostcodeData postcodeData = postcodeDataClient.getPostcodeData(claim);

        assertThat(postcodeData).isEqualTo(PostcodeData.NOT_FOUND);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
    }

    @Test
    void shouldThrowExceptionIfPostcodeDataResponseIsNull() {
        Claim claim = aClaimWithPostcodeData(null);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeDataResponse response = new PostcodeDataResponse(null);
        given(restTemplate.getForObject(anyString(), eq(PostcodeDataResponse.class))).willReturn(response);

        PostcodesIoClientException exception = catchThrowableOfType(() -> postcodeDataClient.getPostcodeData(claim), PostcodesIoClientException.class);

        assertThat(exception).isNotNull();
        assertThat(claim.getPostcodeData()).isNull();
        verify(restTemplate).getForObject(getExpectedPostcodeUrl(postcode), PostcodeDataResponse.class);
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
