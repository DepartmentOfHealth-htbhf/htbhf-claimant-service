package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.AdditionalPregnancyVoucherCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import java.time.LocalDate;
import java.util.Optional;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.TOP_UP_PAYMENT;

@Slf4j
@Component
public class AdditionalPregnancyPaymentMessageProcessor implements MessageTypeProcessor {

    private final MessageContextLoader messageContextLoader;
    private final AdditionalPregnancyVoucherCalculator additionalPregnancyVoucherCalculator;
    private final Integer voucherValueInPence;
    private final PaymentService paymentService;
    private final ReportPaymentMessageSender reportPaymentMessageSender;

    public AdditionalPregnancyPaymentMessageProcessor(@Value("${entitlement.voucher-value-in-pence}") Integer voucherValueInPence,
                                                      MessageContextLoader messageContextLoader,
                                                      AdditionalPregnancyVoucherCalculator additionalPregnancyVoucherCalculator,
                                                      PaymentService paymentService,
                                                      ReportPaymentMessageSender reportPaymentMessageSender) {
        this.messageContextLoader = messageContextLoader;
        this.additionalPregnancyVoucherCalculator = additionalPregnancyVoucherCalculator;
        this.voucherValueInPence = voucherValueInPence;
        this.paymentService = paymentService;
        this.reportPaymentMessageSender = reportPaymentMessageSender;
    }

    /**
     * Processes ADDITIONAL_PREGNANCY_PAYMENT messages from the message queue by calculating and making an additional ad hoc
     * payment when we have been notified of a pregnancy.
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
            log.debug("Making no additional payments as there is no payment cycle or it already contains pregnancy vouchers.");
            return COMPLETED;
        }

        int paymentAmountInPence = calculatePaymentAmountInPence(message, context, paymentCycle.get());
        if (paymentAmountInPence > 0) {
            paymentService.makePayment(paymentCycle.get(), paymentAmountInPence, TOP_UP_PAYMENT);
            reportPaymentMessageSender.sendReportPregnancyTopUpPaymentMessage(paymentCycle.get().getClaim(), paymentCycle.get(), paymentAmountInPence);
        }

        return COMPLETED;
    }

    private boolean hasPregnancyVouchers(Optional<PaymentCycle> paymentCycle) {
        return paymentCycle.get().getVoucherEntitlement().getVouchersForPregnancy() > 0;
    }

    private int calculatePaymentAmountInPence(Message message, AdditionalPregnancyPaymentMessageContext context, PaymentCycle paymentCycle) {
        LocalDate expectedDeliveryDate = context.getClaim().getClaimant().getExpectedDeliveryDate();
        LocalDate claimUpdatedDate = message.getCreatedTimestamp().toLocalDate();
        int numberOfVouchers = additionalPregnancyVoucherCalculator.getAdditionalPregnancyVouchers(
                expectedDeliveryDate,
                paymentCycle,
                claimUpdatedDate);
        return voucherValueInPence * numberOfVouchers;
    }

    @Override
    public MessageType supportsMessageType() {
        return ADDITIONAL_PREGNANCY_PAYMENT;
    }
}
