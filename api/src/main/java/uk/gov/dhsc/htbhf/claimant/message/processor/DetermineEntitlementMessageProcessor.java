package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@Slf4j
@Component
@AllArgsConstructor
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private EligibilityService eligibilityService;

    private MessageContextLoader messageContextLoader;

    private PaymentCycleRepository paymentCycleRepository;

    private MessageQueueClient messageQueueClient;

    @Override
    public MessageType supportsMessageType() {
        return DETERMINE_ENTITLEMENT;
    }

    /**
     * Processes DETERMINE_ENTITLEMENT messages from the message queue by determining the eligibility of the
     * claimant for the current Payment Cycle, then calculating their entitlement accordingly. The entitlement
     * and eligibility responses are then persisted to the current Payment Cycle.
     *
     * @param message The message to process.
     * @return The message status on completion
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {

        DetermineEntitlementMessageContext messageContext = messageContextLoader.loadDetermineEntitlementContext(message);
        Claimant claimant = messageContext.getClaim().getClaimant();
        PaymentCycle currentPaymentCycle = messageContext.getCurrentPaymentCycle();
        PaymentCycle previousPaymentCycle = messageContext.getPreviousPaymentCycle();

        EligibilityAndEntitlementDecision decision = eligibilityService.determineEligibilityAndEntitlementForExistingClaimant(
                claimant,
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle);

        //TODO HTBHF-1296 - update ClaimStatus from ACTIVE to PENDING_EXPIRY if Claimant is no longer eligible.
        updateAndSaveCurrentPaymentCycle(currentPaymentCycle, decision);

        if (decision.getEligibilityStatus() == ELIGIBLE) {
            messageQueueClient.sendMessage(buildMakePaymentMessagePayload(currentPaymentCycle), MAKE_PAYMENT);
        }

        return COMPLETED;
    }

    private void updateAndSaveCurrentPaymentCycle(PaymentCycle currentPaymentCycle,
                                                  EligibilityAndEntitlementDecision decision) {

        currentPaymentCycle.setVoucherEntitlement(decision.getVoucherEntitlement());
        currentPaymentCycle.setEligibilityStatus(decision.getEligibilityStatus());

        paymentCycleRepository.save(currentPaymentCycle);
    }
}
