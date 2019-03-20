package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.controller.ErrorResponse;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClaimantRepository claimantRepository;

    @MockBean
    RestTemplate restTemplateWithIdHeaders;

    URI endpointUrl = URI.create("/v1/claims");

    void assertErrorResponse(ResponseEntity<ErrorResponse> response, String expectedErrorMessage, HttpStatus status) {
        AssertionsForInterfaceTypes.assertThat(response.getStatusCode()).isEqualTo(status);
        ErrorResponse body = response.getBody();
        assertThat(body.getRequestId()).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(status.value());
        assertThat(body.getMessage()).isEqualTo(expectedErrorMessage);
    }

    void assertClaimantMatchesClaimantDTO(ClaimantDTO claimant, Claimant persistedClaim) {
        assertThat(persistedClaim.getNino()).isEqualTo(claimant.getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimant.getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimant.getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimant.getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimant.getExpectedDeliveryDate());
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimant.getCardDeliveryAddress());
    }

    private void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
