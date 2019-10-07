package uk.gov.dhsc.htbhf.claimant.message.processor;

import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;

/**
 * Responsible for reporting new and updated claims for MI reporting purposes.
 * Fetches location data for a claim if it doesn't already exist, then reports the claim.
 */
public class ReportClaimMessageProcessor implements MessageTypeProcessor {

    @Override
    public MessageStatus processMessage(Message message) {
        // TODO DW HTBHF-2412 Fetch location data for a claim and report the claim.
        return null;
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.REPORT_CLAIM;
    }
}
