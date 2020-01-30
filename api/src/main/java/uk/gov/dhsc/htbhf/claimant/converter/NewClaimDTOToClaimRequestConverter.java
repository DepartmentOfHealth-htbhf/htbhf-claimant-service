package uk.gov.dhsc.htbhf.claimant.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;
import uk.gov.dhsc.htbhf.claimant.model.NewClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;

@Component
@RequiredArgsConstructor
public class NewClaimDTOToClaimRequestConverter {

    private final ClaimantDTOToClaimantConverter claimantConverter;

    public ClaimRequest convert(NewClaimDTO dto) {
        Claimant claimant = claimantConverter.convert(dto.getClaimant());
        EligibilityOverride eligibilityOverride = convertToEligibilityOverride(dto.getEligibilityOverride());

        return ClaimRequest.builder()
                .claimant(claimant)
                .deviceFingerprint(dto.getDeviceFingerprint())
                .webUIVersion(dto.getWebUIVersion())
                .eligibilityOverride(eligibilityOverride)
                .build();
    }

    private EligibilityOverride convertToEligibilityOverride(EligibilityOverrideDTO dto) {
        if (dto == null) {
            return null;
        }
        return EligibilityOverride.builder()
                .eligibilityOutcome(dto.getEligibilityOutcome())
                .overrideUntil(dto.getOverrideUntil())
                .childrenDob(dto.getChildrenDob())
                .qualifyingBenefits(dto.getQualifyingBenefits())
                .build();
    }
}
