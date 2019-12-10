package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;

/**
 * Converts a {@link AddressDTOV3} into a {@link Address}.
 */
@Component
public class AddressToAddressDTOConverter {

    public AddressDTOV3 convert(Address source) {
        Assert.notNull(source, "source AddressDTO must not be null");
        return AddressDTOV3.builder()
                .addressLine1(source.getAddressLine1())
                .addressLine2(source.getAddressLine2())
                .townOrCity(source.getTownOrCity())
                .postcode(source.getPostcode())
                .county(source.getCounty())
                .build();

    }
}
