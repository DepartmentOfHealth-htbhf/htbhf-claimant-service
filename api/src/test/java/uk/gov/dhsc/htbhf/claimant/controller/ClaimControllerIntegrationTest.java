package uk.gov.dhsc.htbhf.claimant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimantRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.service.EligibilityClient.ELIGIBILITY_ENDPOINT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityResponseTestDataFactory.anEligibilityResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonDTOTestDataFactory.aValidPerson;

@SpringBootTest
@Transactional
public class ClaimControllerIntegrationTest {

    @Value("${eligibility.base-uri}")
    String baseUri;

    @MockBean
    RestTemplate restTemplateWithIdHeaders;

    @Autowired
    ClaimController controller;

    @Autowired
    ClaimantRepository claimantRepository;

    @Test
    void shouldPersistNewClaimantWithoutOptionalFields() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTO();
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(anEligibilityResponse(), HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(response);

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
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimDTO.getClaimant().getCardDeliveryAddress());
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, aValidPerson(), EligibilityResponse.class);
    }

    @Test
    void shouldPersistNewClaimantWithAllFields() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields();
        ResponseEntity<EligibilityResponse> response = new ResponseEntity<>(anEligibilityResponse(), HttpStatus.OK);
        given(restTemplateWithIdHeaders.postForEntity(anyString(), any(), eq(EligibilityResponse.class))).willReturn(response);

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
        assertThat(persistedClaim.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertAddressEqual(persistedClaim.getCardDeliveryAddress(), claimDTO.getClaimant().getCardDeliveryAddress());
        verify(restTemplateWithIdHeaders).postForEntity(baseUri + ELIGIBILITY_ENDPOINT, aValidPerson(), EligibilityResponse.class);
    }

    private void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
