package uk.gov.dhsc.htbhf.claimant.converter;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponseDTO;

import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
public class ClaimToClaimResponseDTOConverter {

    public List<ClaimResponseDTO> convert(List<Claim> claims) {
        List<ClaimResponseDTO> listOfClaims = new ArrayList<>();
        for (Claim claim : claims) {
            listOfClaims.add(ClaimResponseDTO.builder()
                    .id(claim.getId())
                    .claimStatus(claim.getClaimStatus())
                    .firstName(claim.getClaimant().getFirstName())
                    .lastName(claim.getClaimant().getLastName())
                    .dateOfBirth(claim.getClaimant().getDateOfBirth())
                    .postcode(claim.getClaimant().getAddress().getPostcode())
                    .reference(claim.getReference())
                    .addressLine1(claim.getClaimant().getAddress().getAddressLine1())
                    .build());
        }
        return listOfClaims;
    }
}
