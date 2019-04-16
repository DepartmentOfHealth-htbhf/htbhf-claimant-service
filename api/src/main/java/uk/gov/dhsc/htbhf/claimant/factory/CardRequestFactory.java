package uk.gov.dhsc.htbhf.claimant.factory;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.converter.AddressToAddressDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;

/**
 * Creates a {@link CardRequest} instance from a given {@link Claim}.
 */
@Service
@AllArgsConstructor
public class CardRequestFactory {

    private AddressToAddressDTOConverter addressConverter;

    public CardRequest createCardRequest(Claim claim) {
        Assert.notNull(claim, "claim Claim must not be null");
        AddressDTO address = addressConverter.convert(claim.getClaimant().getCardDeliveryAddress());
        return CardRequest.builder()
                .address(address)
                .firstName(claim.getClaimant().getFirstName())
                .lastName(claim.getClaimant().getLastName())
                .dateOfBirth(claim.getClaimant().getDateOfBirth())
                .claimId(claim.getId().toString())
                .build();

    }
}
