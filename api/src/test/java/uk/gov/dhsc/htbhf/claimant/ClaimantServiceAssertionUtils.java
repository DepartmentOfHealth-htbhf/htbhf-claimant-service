package uk.gov.dhsc.htbhf.claimant;

import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ClaimantServiceAssertionUtils {

    public static final URI CLAIMANT_ENDPOINT_URI = URI.create("/v1/claims");

    public static void assertErrorResponse(ResponseEntity<ErrorResponse> response, String expectedErrorMessage, HttpStatus status) {
        AssertionsForInterfaceTypes.assertThat(response.getStatusCode()).isEqualTo(status);
        ErrorResponse body = response.getBody();
        assertThat(body.getRequestId()).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(status.value());
        assertThat(body.getMessage()).isEqualTo(expectedErrorMessage);
    }

    public static void assertClaimantMatchesClaimantDTO(ClaimantDTO claimant, Claimant persistedClaim) {
        assertThat(persistedClaim.getNino()).isEqualTo(claimant.getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimant.getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimant.getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimant.getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimant.getExpectedDeliveryDate());
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimant.getCardDeliveryAddress());
    }

    private static void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
