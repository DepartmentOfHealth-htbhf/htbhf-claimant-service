package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimantDTOV3;

import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * Converts a {@link ClaimantDTOV3} into a {@link Claimant}.
 */
@Component
public class ClaimantDTOToClaimantConverter {

    private final AddressDTOToAddressConverter addressConverter;

    public ClaimantDTOToClaimantConverter(AddressDTOToAddressConverter addressConverter) {
        this.addressConverter = addressConverter;
    }

    public Claimant convert(ClaimantDTOV3 claimant) {
        Assert.notNull(claimant, "source ClaimantDTO must not be null");
        return Claimant.builder()
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .dateOfBirth(claimant.getDateOfBirth())
                .expectedDeliveryDate(claimant.getExpectedDeliveryDate())
                .nino(upperCase(claimant.getNino()))
                .phoneNumber(claimant.getPhoneNumber())
                .emailAddress(claimant.getEmailAddress())
                .address(addressConverter.convert(claimant.getAddress()))
                .childrenDob(claimant.getChildrenDob())
                .build();
    }

}
