package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.reporting.MIReporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REPORT_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ReportPaymentMessageContextTestDataFactory.aValidReportPaymentMessageContext;

@ExtendWith(MockitoExtension.class)
class ReportPaymentMessageProcessorTest {

    @Mock
    private MIReporter miReporter;
    @Mock
    private MessageContextLoader messageContextLoader;

    @InjectMocks
    private ReportPaymentMessageProcessor reportPaymentMessageProcessor;

    @Test
    void shouldInvokeMIReporter() {
        Message message = aValidMessageWithType(REPORT_PAYMENT);
        ReportPaymentMessageContext context = aValidReportPaymentMessageContext();
        given(messageContextLoader.loadReportPaymentMessageContext(any())).willReturn(context);

        MessageStatus messageStatus = reportPaymentMessageProcessor.processMessage(message);

        assertThat(messageStatus).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadReportPaymentMessageContext(message);
        verify(miReporter).reportPayment(context);
    }
}
