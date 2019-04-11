package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private VoucherEntitlement voucherEntitlement;
}