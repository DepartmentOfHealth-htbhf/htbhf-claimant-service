package uk.gov.dhsc.htbhf.claimant.factory;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.converter.AddressToAddressDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;

@Service
@AllArgsConstructor
public class CardRequestFactory {

    private AddressToAddressDTOConverter addressConverter;

    public CardRequest createCardRequest(Claim claim) {
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
