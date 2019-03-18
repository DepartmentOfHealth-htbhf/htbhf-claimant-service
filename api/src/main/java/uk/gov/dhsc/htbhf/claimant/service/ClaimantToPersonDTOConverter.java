package uk.gov.dhsc.htbhf.claimant.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

@Component
public class ClaimantToPersonDTOConverter implements Converter<Claimant, PersonDTO> {

    @Override
    public PersonDTO convert(Claimant claimant) {
        return PersonDTO.builder()
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .dateOfBirth(claimant.getDateOfBirth())
                .nino(claimant.getNino())
                .address(convertAddress(claimant.getCardDeliveryAddress()))
                .build();
    }

    private AddressDTO convertAddress(Address address) {
        return AddressDTO.builder()
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .townOrCity(address.getTownOrCity())
                .postcode(address.getPostcode())
                .build();
    }
}
