package uk.gov.dhsc.htbhf.claimant.factory;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.converter.AddressToAddressDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;

/**
 * Creates a {@link CardRequest} instance from a given {@link Claim}.
 */
@Service
@AllArgsConstructor
public class CardRequestFactory {

    private AddressToAddressDTOConverter addressConverter;

    public CardRequest createCardRequest(Claim claim) {
        Assert.notNull(claim, "Claim must not be null");
        Claimant claimant = claim.getClaimant();
        AddressDTO address = addressConverter.convert(claimant.getAddress());
        return CardRequest.builder()
                .address(address)
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .dateOfBirth(claimant.getDateOfBirth())
                .email(claimant.getEmailAddress())
                .mobile(claimant.getPhoneNumber())
                .claimId(claim.getId().toString())
                .build();

    }
}
