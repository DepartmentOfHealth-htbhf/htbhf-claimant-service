package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;

import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * Converts a {@link AddressDTOV3} into a {@link Address}.
 */
@Component
public class AddressDTOToAddressConverter {

    public Address convert(AddressDTOV3 source) {
        Assert.notNull(source, "source AddressDTO must not be null");
        return Address.builder()
                .addressLine1(source.getAddressLine1())
                .addressLine2(source.getAddressLine2())
                .townOrCity(source.getTownOrCity())
                .county(source.getCounty())
                .postcode(upperCase(source.getPostcode()))
                .build();

    }
}
