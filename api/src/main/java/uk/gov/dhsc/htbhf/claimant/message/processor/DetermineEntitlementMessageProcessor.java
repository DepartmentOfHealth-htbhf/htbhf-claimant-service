package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.message.*;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@Slf4j
@Component
@AllArgsConstructor
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private EligibilityAndEntitlementService eligibilityAndEntitlementService;

    private MessageContextLoader messageContextLoader;

    private PaymentCycleService paymentCycleService;

    private MessageQueueClient messageQueueClient;

    @Override
    public MessageType supportsMessageType() {
        return DETERMINE_ENTITLEMENT;
    }

    /**
     * Processes DETERMINE_ENTITLEMENT messages from the message queue by determining the eligibility and entitlement of the
     * claimant for the current Payment Cycle. The entitlement and eligibility are then persisted to the current Payment Cycle.
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

        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateExistingClaimant(
                claimant,
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle);

        //TODO HTBHF-1296 - update ClaimStatus from ACTIVE to PENDING_EXPIRY if Claimant is no longer eligible.
        updateAndSavePaymentCycle(currentPaymentCycle, decision);

        if (decision.getEligibilityStatus() == ELIGIBLE) {
            createMakePaymentMessage(currentPaymentCycle);
        }

        return COMPLETED;
    }

    private void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    private void updateAndSavePaymentCycle(PaymentCycle paymentCycle, EligibilityAndEntitlementDecision decision) {
        paymentCycle.setEligibilityStatus(decision.getEligibilityStatus());
        paymentCycle.setChildrenDob(decision.getDateOfBirthOfChildren());
        paymentCycle.applyVoucherEntitlement(decision.getVoucherEntitlement());
        PaymentCycleStatus paymentCycleStatus = PaymentCycleStatus.getStatusForEligibilityDecision(decision.getEligibilityStatus());
        paymentCycle.setPaymentCycleStatus(paymentCycleStatus);
        LocalDate expectedDeliveryDate = paymentCycleService.getExpectedDeliveryDateIfRelevant(paymentCycle.getClaim(), decision.getVoucherEntitlement());
        paymentCycle.setExpectedDeliveryDate(expectedDeliveryDate);
        paymentCycleService.savePaymentCycle(paymentCycle);
    }

}
