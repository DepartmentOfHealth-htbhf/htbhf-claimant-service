package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EligibilityAndEntitlementDecision {

    private final EligibilityStatus eligibilityStatus;
    private final String dwpHouseholdIdentifier;
    private final String hmrcHouseholdIdentifier;
    private final List<LocalDate> dateOfBirthOfChildren;
    private final PaymentCycleVoucherEntitlement voucherEntitlement;

    public static EligibilityAndEntitlementDecision buildWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }
}
