package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import java.util.Optional;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;

@Slf4j
@Component
@AllArgsConstructor
public class AdditionalPregnancyPaymentMessageProcessor implements MessageTypeProcessor {

    private MessageContextLoader messageContextLoader;
    private PaymentService paymentService;

    /**
     * Processes ADDITIONAL_PREGNANCY_PAYMENT messages from the message queue by calculating and making an additional ad hoc
     * payment when we have been notified of a pregnancy.
     * TODO review/expand javadoc
     *
     * @param message The message to process.
     * @return The message status on completion
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        AdditionalPregnancyPaymentMessageContext context = messageContextLoader.loadAdditionalPregnancyPaymentMessageContext(message);
        Optional<PaymentCycle> paymentCycle = context.getPaymentCycle();
        if (paymentCycle.isEmpty() || hasPregnancyVouchers(paymentCycle)) {
            return COMPLETED;
        }

        return COMPLETED;
    }

    private boolean hasPregnancyVouchers(Optional<PaymentCycle> paymentCycle) {
        return paymentCycle.get().getVoucherEntitlement().getVouchersForPregnancy() > 0;
    }

    @Override
    public MessageType supportsMessageType() {
        return ADDITIONAL_PREGNANCY_PAYMENT;
    }
}
