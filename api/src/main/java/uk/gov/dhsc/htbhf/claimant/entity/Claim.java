package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

@Data
@Builder
@AllArgsConstructor
public class Claim {

    private Claimant claimant;
    private VoucherEntitlement voucherEntitlement;
}
