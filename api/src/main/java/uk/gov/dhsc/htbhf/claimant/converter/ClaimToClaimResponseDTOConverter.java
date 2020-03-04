package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponseDTO;

import java.util.ArrayList;
import java.util.List;


@Component
public class ClaimToClaimResponseDTOConverter {

    public List<ClaimResponseDTO> convert(Iterable<Claim> claims) {
        List<ClaimResponseDTO> claimResponseList = new ArrayList<>();
        for (Claim claim : claims) {
            claimResponseList.add(aValidClaimResponseDTO(claim));
        }
        return claimResponseList;
    }

    private ClaimResponseDTO aValidClaimResponseDTO(Claim claim) {
        Claimant claimant = claim.getClaimant();
        return ClaimResponseDTO.builder()
                .id(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .dateOfBirth(claimant.getDateOfBirth())
                .postcode(claimant.getAddress().getPostcode())
                .reference(claim.getReference())
                .addressLine1(claimant.getAddress().getAddressLine1())
                .build();
    }
}
