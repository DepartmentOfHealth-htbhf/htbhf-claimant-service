package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;

public class ClaimResultDTOTestDataFactory {

    public static ClaimResultDTO aClaimResultDTOWithClaimStatus(ClaimStatus claimStatus) {
        return aValidClaimResultDTOBuilder().claimStatus(claimStatus).build();
    }

    public static ClaimResultDTO aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(ClaimStatus claimStatus) {
        return aValidClaimResultDTOBuilder()
                .voucherEntitlement(null)
                .claimStatus(claimStatus)
                .build();
    }

    private static ClaimResultDTO.ClaimResultDTOBuilder aValidClaimResultDTOBuilder() {
        return ClaimResultDTO.builder()
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .claimStatus(ClaimStatus.NEW)
                .voucherEntitlement(aValidVoucherEntitlementDTO())
                .verificationResult(anAllMatchedVerificationResult());
    }
}
