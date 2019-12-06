package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReportPaymentMessageContext extends ReportEventMessageContext {
    private PaymentCycle paymentCycle;
    private PaymentAction paymentAction;
    private int paymentForPregnancy;
    private int paymentForChildrenUnderOne;
    private int paymentForChildrenBetweenOneAndFour;
    private int paymentForBackdatedVouchers;

    @Override
    public String getEventAction() {
        return paymentAction.name();
    }
}
