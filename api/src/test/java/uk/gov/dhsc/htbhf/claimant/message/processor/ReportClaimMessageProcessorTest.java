package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.reporting.MIReporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;

@ExtendWith(MockitoExtension.class)
class ReportClaimMessageProcessorTest {

    @Mock
    private MIReporter miReporter;
    @Mock
    private MessageContextLoader messageContextLoader;

    @InjectMocks
    private ReportClaimMessageProcessor reportClaimMessageProcessor;

    @Test
    void shouldInvokeMIReporter() {
        Claim claim = aValidClaim();
        Message message = aValidMessageWithType(REPORT_CLAIM);
        ReportClaimMessageContext context = ReportClaimMessageContext.builder().claim(claim).build();
        given(messageContextLoader.loadReportClaimMessageContext(any())).willReturn(context);

        MessageStatus messageStatus = reportClaimMessageProcessor.processMessage(message);

        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadReportClaimMessageContext(message);
        verify(miReporter).reportClaim(claim);
    }
}
