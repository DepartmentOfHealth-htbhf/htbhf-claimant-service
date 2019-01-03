package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;

@Component
public class ClaimDTOToClaimConverter {

    public Claim convert(ClaimDTO source) {
        return Claim.builder().claimant(
                Claimant.builder()
                        .firstName(source.getClaimant().getFirstName())
                        .secondName(source.getClaimant().getSecondName())
                        .build()
        )
                .build();
    }
}
