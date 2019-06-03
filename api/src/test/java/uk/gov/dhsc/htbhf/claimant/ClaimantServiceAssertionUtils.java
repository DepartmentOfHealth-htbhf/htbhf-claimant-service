package uk.gov.dhsc.htbhf.claimant;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ClaimantServiceAssertionUtils {

    public static final URI CLAIMANT_ENDPOINT_URI = URI.create("/v2/claims");

    public static void assertClaimantMatchesClaimantDTO(ClaimantDTO claimant, Claimant persistedClaim) {
        assertThat(persistedClaim.getNino()).isEqualTo(claimant.getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimant.getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimant.getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimant.getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimant.getExpectedDeliveryDate());
        assertAddressEqual(persistedClaim.getAddress(), claimant.getAddress());
    }

    private static void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
