package uk.gov.dhsc.htbhf.claimant.service;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithoutEligibilityStatus;

class ClaimantToPersonDTOConverterTest {

    private ClaimantToPersonDTOConverter converter = new ClaimantToPersonDTOConverter();

    @Test
    void shouldConvertValidClaimant() {
        Claimant claimant = aClaimantWithoutEligibilityStatus();

        PersonDTO person = converter.convert(claimant);

        assertThat(person).isNotNull();
        assertThat(person.getFirstName()).isEqualTo(claimant.getFirstName());
        assertThat(person.getLastName()).isEqualTo(claimant.getLastName());
        assertThat(person.getDateOfBirth()).isEqualTo(claimant.getDateOfBirth());
        assertThat(person.getNino()).isEqualTo(claimant.getNino());
        assertThat(person.getNino()).isEqualTo(claimant.getNino());
        AddressDTO personAddress = person.getAddress();
        assertThat(personAddress).isNotNull();
        Address claimantAddress = claimant.getCardDeliveryAddress();
        assertThat(personAddress.getAddressLine1()).isEqualTo(claimantAddress.getAddressLine1());
        assertThat(personAddress.getAddressLine2()).isEqualTo(claimantAddress.getAddressLine2());
        assertThat(personAddress.getTownOrCity()).isEqualTo(claimantAddress.getTownOrCity());
        assertThat(personAddress.getPostcode()).isEqualTo(claimantAddress.getPostcode());
    }
}
