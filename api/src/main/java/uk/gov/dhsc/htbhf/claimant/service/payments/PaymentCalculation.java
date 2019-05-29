package uk.gov.dhsc.htbhf.claimant.service.payments;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;

/**
 * The result of a payment calculation from {@link PaymentCalculator}.
 */
@Data
@Builder
public class PaymentCalculation {

    private PaymentCycleStatus paymentCycleStatus;

    private int paymentAmount;
}
