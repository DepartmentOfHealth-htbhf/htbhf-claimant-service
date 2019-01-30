package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

/**
 * Converts a {@link ClaimDTO} into a {@link Claim}.
 */
@Component
public class ClaimDTOToClaimConverter {

    private final AddressDTOToAddressConverter addressConverter;

    public ClaimDTOToClaimConverter(AddressDTOToAddressConverter addressConverter) {
        this.addressConverter = addressConverter;
    }

    public Claim convert(ClaimDTO source) {
        Assert.notNull(source, "source ClaimDTO must not be null");
        return Claim.builder()
                .claimant(buildClaimant(source.getClaimant()))
                .build();
    }

    private Claimant buildClaimant(ClaimantDTO claimant) {
        return Claimant.builder()
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .dateOfBirth(claimant.getDateOfBirth())
                .nino(claimant.getNino())
                .cardDeliveryAddress(convertCardDeliveryAddress(claimant))
                .build();
    }

    private Address convertCardDeliveryAddress(ClaimantDTO claimant) {
        return claimant.getCardDeliveryAddress() == null
                ? null
                : addressConverter.convert(claimant.getCardDeliveryAddress());
    }
}
