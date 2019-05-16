package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityService;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;

@Slf4j
@Component
@AllArgsConstructor
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private MessageRepository messageRepository;

    private EligibilityService eligibilityService;

    private CycleEntitlementCalculator cycleEntitlementCalculator;

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
        Claim claim = messageContext.getClaim();
        Claimant claimant = claim.getClaimant();
        PaymentCycle currentPaymentCycle = messageContext.getCurrentPaymentCycle();
        PaymentCycle previousPaymentCycle = messageContext.getPreviousPaymentCycle();

        EligibilityResponse eligibilityResponse = eligibilityService.determineEligibility(claimant);
        PaymentCycleVoucherEntitlement voucherEntitlement = null;
        if (EligibilityStatus.ELIGIBLE == eligibilityResponse.getEligibilityStatus()) {
            voucherEntitlement = determineVoucherEntitlement(
                    currentPaymentCycle,
                    previousPaymentCycle,
                    eligibilityResponse,
                    claimant);
            MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(currentPaymentCycle);
            messageQueueClient.sendMessage(messagePayload, MAKE_PAYMENT);
        }

        updateAndSaveCurrentPaymentCycle(currentPaymentCycle, eligibilityResponse, voucherEntitlement);

        messageRepository.delete(message);
        return COMPLETED;
    }

    private PaymentCycleVoucherEntitlement determineVoucherEntitlement(PaymentCycle currentPaymentCycle,
                                                                       PaymentCycle previousPaymentCycle,
                                                                       EligibilityResponse eligibilityResponse,
                                                                       Claimant claimant) {

        Optional<LocalDate> expectedDeliveryDate = Optional.ofNullable(claimant.getExpectedDeliveryDate());
        List<LocalDate> dateOfBirthOfChildren = eligibilityResponse.getDateOfBirthOfChildren();
        return cycleEntitlementCalculator.calculateEntitlement(
                expectedDeliveryDate,
                dateOfBirthOfChildren,
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle.getVoucherEntitlement());
    }

    private void updateAndSaveCurrentPaymentCycle(PaymentCycle currentPaymentCycle,
                                                  EligibilityResponse eligibilityResponse,
                                                  PaymentCycleVoucherEntitlement paymentCycleVoucherEntitlement) {

        currentPaymentCycle.setVoucherEntitlement(paymentCycleVoucherEntitlement);
        currentPaymentCycle.setEligibilityStatus(eligibilityResponse.getEligibilityStatus());

        paymentCycleRepository.save(currentPaymentCycle);
    }
}
