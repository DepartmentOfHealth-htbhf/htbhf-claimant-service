package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;

import java.util.Optional;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private Optional<VoucherEntitlement> voucherEntitlement;
    private VerificationResult verificationResult;

    public static ClaimResult withNoEntitlement(Claim claim) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .build();
    }

    public static ClaimResult withNoEntitlement(Claim claim,
                                                VerificationResult verificationResult) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .verificationResult(verificationResult)
                .build();
    }

    public static ClaimResult withEntitlement(Claim claim,
                                              VoucherEntitlement voucherEntitlement,
                                              VerificationResult verificationResult) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .verificationResult(verificationResult)
                .build();
    }
}
