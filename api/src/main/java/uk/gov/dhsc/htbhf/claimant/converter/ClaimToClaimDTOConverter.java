package uk.gov.dhsc.htbhf.claimant.converter;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

@Component
@AllArgsConstructor
public class ClaimToClaimDTOConverter {

    private final AddressToAddressDTOConverter addressToAddressDTOConverter;
    private final EligibilityOverrideToEligibilityOverrideDTOConverter eligibilityOverrideDTOConverter;

    public ClaimDTO convert(Claim claim) {
        return ClaimDTO.builder()
                .id(claim.getId())
                .cardAccountId(claim.getCardAccountId())
                .cardStatus(claim.getCardStatus())
                .cardStatusTimestamp(claim.getCardStatusTimestamp())
                .claimStatus(claim.getClaimStatus())
                .claimStatusTimestamp(claim.getClaimStatusTimestamp())
                .currentIdentityAndEligibilityResponse(claim.getCurrentIdentityAndEligibilityResponse())
                .dwpHouseholdIdentifier(claim.getDwpHouseholdIdentifier())
                .eligibilityStatus(claim.getEligibilityStatus())
                .eligibilityStatusTimestamp(claim.getEligibilityStatusTimestamp())
                .hmrcHouseholdIdentifier(claim.getHmrcHouseholdIdentifier())
                .initialIdentityAndEligibilityResponse(claim.getInitialIdentityAndEligibilityResponse())
                .claimant(convert(claim.getClaimant()))
                .eligibilityOverride(eligibilityOverrideDTOConverter.convert(claim.getEligibilityOverride()))
                .reference(claim.getReference())
                .build();
    }

    private ClaimantDTO convert(Claimant claimant) {
        AddressDTO addressDTO = addressToAddressDTOConverter.convert(claimant.getAddress());
        return ClaimantDTO.builder()
                .initiallyDeclaredChildrenDob(claimant.getInitiallyDeclaredChildrenDob())
                .dateOfBirth(claimant.getDateOfBirth())
                .expectedDeliveryDate(claimant.getExpectedDeliveryDate())
                .address(addressDTO)
                .emailAddress(claimant.getEmailAddress())
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .nino(claimant.getNino())
                .phoneNumber(claimant.getPhoneNumber())
                .build();
    }
}
