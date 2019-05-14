package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.DetermineEntitlementMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.UUID;
import javax.transaction.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class CreateNewPaymentCycleJob {

    private final PaymentCycleService paymentCycleService;
    private final ClaimRepository claimRepository;
    private final MessageQueueClient messageQueue;

    /**
     * Creates and saves a new {@link PaymentCycle},
     * and puts a DETERMINE_ENTITLEMENT message on the queue for that cycle.
     * @param claimId the id of the claim the new cycle is for.
     * @param previousCycleId The id of the previous payment cycle.
     * @param cycleStartDate the start date of the new cycle.
     * @return the new cycle.
     */
    @Transactional
    public PaymentCycle createNewPaymentCycle(UUID claimId, UUID previousCycleId, LocalDate cycleStartDate) {

        Claim claim = claimRepository.getLazyLoadingClaim(claimId);
        PaymentCycle cycle = paymentCycleService.createAndSavePaymentCycle(claim, cycleStartDate);

        DetermineEntitlementMessagePayload messagePayload = DetermineEntitlementMessagePayload.builder()
                .claimId(claimId)
                .currentPaymentCycleId(cycle.getId())
                .previousPaymentCycleId(previousCycleId)
                .build();

        messageQueue.sendMessage(messagePayload, MessageType.DETERMINE_ENTITLEMENT);

        return cycle;
    }
}
