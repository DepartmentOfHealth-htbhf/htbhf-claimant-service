package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class NewCardMessageContext {

    private Claim claim;
    private PaymentCycleVoucherEntitlement paymentCycleVoucherEntitlement;
    private List<LocalDate> datesOfBirthOfChildren;
}
