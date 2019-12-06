package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class EligibilityAndEntitlementDecision {

    private final EligibilityStatus eligibilityStatus;
    private final CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse;
    private final UUID existingClaimId;
    private final String dwpHouseholdIdentifier;
    private final String hmrcHouseholdIdentifier;
    private final List<LocalDate> dateOfBirthOfChildren;
    private final PaymentCycleVoucherEntitlement voucherEntitlement;

    public static EligibilityAndEntitlementDecision buildWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    public static EligibilityAndEntitlementDecision buildDuplicateDecisionWithExistingClaimId(UUID existingClaimId) {
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(EligibilityStatus.DUPLICATE)
                .existingClaimId(existingClaimId)
                .build();
    }

    public boolean childrenPresent() {
        return !CollectionUtils.isEmpty(dateOfBirthOfChildren);
    }
}
