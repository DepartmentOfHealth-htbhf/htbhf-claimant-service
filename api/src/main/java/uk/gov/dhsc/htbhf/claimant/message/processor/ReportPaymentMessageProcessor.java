package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.reporting.MIReporter;

/**
 * Responsible for reporting payments for MI reporting purposes.
 */
@Component
@AllArgsConstructor
public class ReportPaymentMessageProcessor implements MessageTypeProcessor {

    private final MIReporter miReporter;
    private final MessageContextLoader messageContextLoader;

    @Override
    public MessageStatus processMessage(Message message) {
        ReportPaymentMessageContext context = messageContextLoader.loadReportPaymentMessageContext(message);
        miReporter.reportPayment(context);
        return MessageStatus.COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.REPORT_PAYMENT;
    }
}
