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
public class EligibilityAndEntitlement {

    private final EligibilityStatus eligibilityStatus;
    private final String dwpHouseholdIdentifier;
    private final String hmrcHouseholdIdentifier;
    private final List<LocalDate> dateOfBirthOfChildren;
    private final PaymentCycleVoucherEntitlement voucherEntitlement;

    public static EligibilityAndEntitlement buildWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityAndEntitlement.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }
}
