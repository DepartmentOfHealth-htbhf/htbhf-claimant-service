package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.reporting.MIReporter;

/**
 * Responsible for reporting new and updated claims for MI reporting purposes.
 * Fetches location data for a claim if it doesn't already exist, then reports the claim.
 */
@Component
@AllArgsConstructor
public class ReportClaimMessageProcessor implements MessageTypeProcessor {

    private final MIReporter miReporter;
    private final MessageContextLoader messageContextLoader;

    @Override
    public MessageStatus processMessage(Message message) {
        ReportClaimMessageContext context = messageContextLoader.loadReportClaimMessageContext(message);
        miReporter.reportClaim(context);
        return MessageStatus.COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.REPORT_CLAIM;
    }
}
