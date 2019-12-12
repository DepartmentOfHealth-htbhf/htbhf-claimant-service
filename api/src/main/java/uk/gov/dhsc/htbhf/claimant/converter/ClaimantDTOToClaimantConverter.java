package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * Converts a {@link ClaimantDTO} into a {@link Claimant}.
 */
@Component
public class ClaimantDTOToClaimantConverter {

    private final AddressDTOToAddressConverter addressConverter;

    public ClaimantDTOToClaimantConverter(AddressDTOToAddressConverter addressConverter) {
        this.addressConverter = addressConverter;
    }

    public Claimant convert(ClaimantDTO claimant) {
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
                .initiallyDeclaredChildrenDob(claimant.getChildrenDob())
                .build();
    }

}
