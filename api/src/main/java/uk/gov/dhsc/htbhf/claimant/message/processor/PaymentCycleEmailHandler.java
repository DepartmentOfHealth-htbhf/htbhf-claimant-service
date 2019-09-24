package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.createPaymentEmailPersonalisationMap;

/**
 * Component responsible for determining if any additional emails need to be sent out in addition to
 * the normal Payment email for a given PaymentCycle.
 */
@Component
@AllArgsConstructor
@Slf4j
public class PaymentCycleEmailHandler {

    private MessageQueueClient messageQueueClient;
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    public void handleAdditionalEmails(PaymentCycle paymentCycle) {
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        if (nextPaymentCycleSummary.hasChildrenTurningFour()) {
            sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        }
    }

    private void sendChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildChildTurnsFourNotificationEmailPayload(
                paymentCycle,
                entitlement,
                multipleChildrenTurningFourInNextMonth);
        log.info("Sending email for child turns 4 for Payment Cycle after cycle with id: [{}]", paymentCycle.getId());
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    private PaymentCycleVoucherEntitlement determineEntitlementForNextCycle(PaymentCycle currentPaymentCycle) {
        LocalDate nextCycleStartDate = currentPaymentCycle.getCycleEndDate().plusDays(1);
        return paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(currentPaymentCycle.getClaim().getClaimant().getExpectedDeliveryDate()),
                currentPaymentCycle.getChildrenDob(),
                nextCycleStartDate,
                currentPaymentCycle.getVoucherEntitlement());
    }

    private EmailMessagePayload buildChildTurnsFourNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                            PaymentCycleVoucherEntitlement entitlementNextMonth,
                                                                            boolean multipleChildrenTurningFourInMonth) {
        Map<String, Object> emailPersonalisation = createPaymentEmailPersonalisationMap(paymentCycle, entitlementNextMonth);
        emailPersonalisation.put(EmailTemplateKey.MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningFourInMonth);
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(EmailType.CHILD_TURNS_FOUR)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

}
