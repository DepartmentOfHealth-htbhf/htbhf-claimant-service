package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;

import java.util.Optional;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReportPaymentMessageContext extends ReportEventMessageContext {

    private Optional<PaymentCycle> paymentCycle;
    private PaymentAction paymentAction;

    @Override
    public String getEventAction() {
        return paymentAction.name();
    }
}
