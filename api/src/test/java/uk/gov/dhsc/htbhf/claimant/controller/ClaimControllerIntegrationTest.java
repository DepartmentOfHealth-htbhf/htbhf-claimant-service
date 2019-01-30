package uk.gov.dhsc.htbhf.claimant.controller;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
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
    void shouldPersistNewClaimant() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTO();

        // When
        controller.newClaim(claimDTO);

        // Then
        Iterable<Claimant> claimants = claimantRepository.findAll();
        assertThat(claimants).hasSize(1);
        Claimant persistedClaim = claimants.iterator().next();
        AssertionsForClassTypes.assertThat(persistedClaim.getNino()).isEqualTo(claimDTO.getClaimant().getNino());
        AssertionsForClassTypes.assertThat(persistedClaim.getFirstName()).isEqualTo(claimDTO.getClaimant().getFirstName());
        AssertionsForClassTypes.assertThat(persistedClaim.getLastName()).isEqualTo(claimDTO.getClaimant().getLastName());
        AssertionsForClassTypes.assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimDTO.getClaimant().getDateOfBirth());
        AssertionsForClassTypes.assertThat(persistedClaim.getExpectedDeliveryDate()).isNull();
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
        AssertionsForClassTypes.assertThat(persistedClaim.getNino()).isEqualTo(claimDTO.getClaimant().getNino());
        AssertionsForClassTypes.assertThat(persistedClaim.getFirstName()).isEqualTo(claimDTO.getClaimant().getFirstName());
        AssertionsForClassTypes.assertThat(persistedClaim.getLastName()).isEqualTo(claimDTO.getClaimant().getLastName());
        AssertionsForClassTypes.assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimDTO.getClaimant().getDateOfBirth());
        AssertionsForClassTypes.assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimDTO.getClaimant().getExpectedDeliveryDate());
    }

}
