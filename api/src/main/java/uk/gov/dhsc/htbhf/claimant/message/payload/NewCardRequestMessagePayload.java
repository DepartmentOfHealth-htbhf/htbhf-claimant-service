package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class NewCardRequestMessagePayload implements MessagePayload {

    private UUID claimId;
    private PaymentCycleVoucherEntitlement voucherEntitlement;
    private List<LocalDate> datesOfBirthOfChildren;
}
