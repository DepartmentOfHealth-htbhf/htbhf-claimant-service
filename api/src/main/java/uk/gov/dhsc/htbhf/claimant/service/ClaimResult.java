package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import org.javers.common.collections.Lists;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private Optional<VoucherEntitlement> voucherEntitlement;
    private Boolean claimUpdated;
    private List<String> updatedFields;

    public static ClaimResult withNoEntitlement(Claim claim) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .build();
    }

    public static ClaimResult withEntitlement(Claim claim, VoucherEntitlement voucherEntitlement) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .build();
    }

    public static ClaimResult withEntitlementAndUpdatedFields(Claim claim, VoucherEntitlement voucherEntitlement, List<UpdatableClaimantField> updatedFields) {
        List<String> updatedFieldsAsStrings = Lists.transform(updatedFields, UpdatableClaimantField::getFieldName);
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .updatedFields(updatedFieldsAsStrings)
                .claimUpdated(true)
                .build();
    }
}
