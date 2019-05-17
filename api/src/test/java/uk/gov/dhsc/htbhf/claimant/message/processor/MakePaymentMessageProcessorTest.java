package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidMakePaymentMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class MakePaymentMessageProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private MessageContextLoader messageContextLoader;

    @InjectMocks
    MakePaymentMessageProcessor processor;

    @Test
    void shouldProcessMessage() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessageContext messageContext = aValidMakePaymentMessageContext(paymentCycle, claim);
        given(messageContextLoader.loadMakePaymentContext(any())).willReturn(messageContext);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadMakePaymentContext(message);
        verify(paymentService).makePayment(paymentCycle, claim.getCardAccountId());
    }

}
