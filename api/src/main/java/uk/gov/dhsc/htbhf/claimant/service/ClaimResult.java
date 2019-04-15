package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.util.Optional;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private Optional<VoucherEntitlement> voucherEntitlement;
}