package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateNewPaymentCycleJobTest {

    @Mock
    PaymentCycleService paymentCycleService;
    @Mock
    ClaimRepository claimRepository;
    @Mock
    MessageQueueClient messageQueue;

    @InjectMocks
    CreateNewPaymentCycleJob job;


    @Test
    void shouldCreatePaymentCycleAndPutMessageOnQueue() {
        UUID previousCycleId = UUID.randomUUID();
        LocalDate cycleStartDate = LocalDate.now();
        Claim claim = Claim.builder().build();
        UUID claimId = claim.getId();
        given(claimRepository.getLazyLoadingClaim(any())).willReturn(claim);
        PaymentCycle paymentCycle = PaymentCycle.builder().build();
        given(paymentCycleService.createAndSavePaymentCycle(any(), any())).willReturn(paymentCycle);

        PaymentCycle result = job.createNewPaymentCycle(claimId, previousCycleId, cycleStartDate);

        assertThat(result).isEqualTo(paymentCycle);
        verify(claimRepository).getLazyLoadingClaim(claimId);
        verify(paymentCycleService).createAndSavePaymentCycle(claim, cycleStartDate);
        ArgumentCaptor<DetermineEntitlementMessagePayload> argumentCaptor = ArgumentCaptor.forClass(DetermineEntitlementMessagePayload.class);
        verify(messageQueue).sendMessage(argumentCaptor.capture(), eq(MessageType.DETERMINE_ENTITLEMENT));
        DetermineEntitlementMessagePayload messagePayload = argumentCaptor.getValue();
        assertThat(messagePayload.getClaimId()).isEqualTo(claimId);
        assertThat(messagePayload.getCurrentPaymentCycleId()).isEqualTo(result.getId());
        assertThat(messagePayload.getPreviousPaymentCycleId()).isEqualTo(previousCycleId);
    }
}