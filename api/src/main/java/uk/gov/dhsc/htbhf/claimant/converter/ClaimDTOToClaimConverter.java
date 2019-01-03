package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;

/**
 * Converts a {@link ClaimDTO} into a {@link Claim}.
 */
@Component
public class ClaimDTOToClaimConverter {

    public Claim convert(ClaimDTO source) {
        Assert.notNull(source, "source ClaimDTO must not be null");
        return Claim.builder()
                .claimant(
                        Claimant.builder()
                                .firstName(source.getClaimant().getFirstName())
                                .secondName(source.getClaimant().getSecondName())
                                .build()
                )
                .build();
    }
}
