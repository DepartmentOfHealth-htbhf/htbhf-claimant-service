package uk.gov.dhsc.htbhf.claimant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ClaimControllerIntegrationTest {

    @Autowired
    ClaimController controller;

    @Autowired
    ClaimantRepository claimantRepository;

    @Test
    void shouldPersistNewClaimantWithoutOptionalFields() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTO();

        // When
        controller.newClaim(claimDTO);

        // Then
        Iterable<Claimant> claimants = claimantRepository.findAll();
        assertThat(claimants).hasSize(1);
        Claimant persistedClaim = claimants.iterator().next();
        assertThat(persistedClaim.getNino()).isEqualTo(claimDTO.getClaimant().getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimDTO.getClaimant().getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimDTO.getClaimant().getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimDTO.getClaimant().getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isNull();
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimDTO.getClaimant().getCardDeliveryAddress());

    }

    @Test
    void shouldPersistNewClaimantWithAllFields() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields();

        // When
        controller.newClaim(claimDTO);

        // Then
        Iterable<Claimant> claimants = claimantRepository.findAll();
        assertThat(claimants).hasSize(1);
        Claimant persistedClaim = claimants.iterator().next();
        assertThat(persistedClaim.getNino()).isEqualTo(claimDTO.getClaimant().getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimDTO.getClaimant().getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimDTO.getClaimant().getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimDTO.getClaimant().getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimDTO.getClaimant().getExpectedDeliveryDate());
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimDTO.getClaimant().getCardDeliveryAddress());
    }

    private void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
