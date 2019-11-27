package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import org.javers.common.collections.Lists;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private Optional<VoucherEntitlement> voucherEntitlement;
    private Boolean claimUpdated;
    private List<String> updatedFields;
    private VerificationResult verificationResult;

    public static ClaimResult withNoEntitlement(Claim claim, IdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .verificationResult(buildVerificationResult(identityAndEligibilityResponse))
                .build();
    }

    public static ClaimResult withEntitlement(Claim claim, VoucherEntitlement voucherEntitlement,
                                              IdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .verificationResult(buildVerificationResult(identityAndEligibilityResponse))
                .build();
    }

    public static ClaimResult withEntitlementAndUpdatedFields(Claim claim, VoucherEntitlement voucherEntitlement, List<UpdatableClaimantField> updatedFields,
                                                              IdentityAndEligibilityResponse identityAndEligibilityResponse) {
        List<String> updatedFieldsAsStrings = Lists.transform(updatedFields, UpdatableClaimantField::getFieldName);
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .updatedFields(updatedFieldsAsStrings)
                .claimUpdated(true)
                .verificationResult(buildVerificationResult(identityAndEligibilityResponse))
                .build();
    }

    private static VerificationResult buildVerificationResult(IdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return VerificationResult.builder()
                .addressLine1Match(identityAndEligibilityResponse.getAddressLine1Match())
                .deathVerificationFlag(identityAndEligibilityResponse.getDeathVerificationFlag())
                .emailAddressMatch(identityAndEligibilityResponse.getEmailAddressMatch())
                .mobilePhoneMatch(identityAndEligibilityResponse.getMobilePhoneMatch())
                .postcodeMatch(identityAndEligibilityResponse.getPostcodeMatch())
                .pregnantChildDOBMatch(identityAndEligibilityResponse.getPregnantChildDOBMatch())
                .qualifyingBenefits(identityAndEligibilityResponse.getQualifyingBenefits())
                .build();
    }
}
