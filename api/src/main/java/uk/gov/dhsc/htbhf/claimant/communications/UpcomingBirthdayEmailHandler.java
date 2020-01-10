package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.formatPaymentAmountSummary;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

/**
 * Component responsible for determining if any additional emails need to be sent out in addition to
 * the normal Payment email for a given PaymentCycle.
 */
@Component
@Slf4j
public class UpcomingBirthdayEmailHandler {

    private final Integer numberOfCalculationPeriods;
    private final Duration changeInPaymentEmailDelay;
    private final MessageQueueClient messageQueueClient;
    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;
    private final EmailMessagePayloadFactory emailMessagePayloadFactory;

    public UpcomingBirthdayEmailHandler(@Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                        @Value("${payment-cycle.change-in-payment-email-delay}") Duration changeInPaymentEmailDelay,
                                        MessageQueueClient messageQueueClient,
                                        PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator,
                                        EmailMessagePayloadFactory emailMessagePayloadFactory) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.changeInPaymentEmailDelay = changeInPaymentEmailDelay;
        this.messageQueueClient = messageQueueClient;
        this.paymentCycleEntitlementCalculator = paymentCycleEntitlementCalculator;
        this.emailMessagePayloadFactory = emailMessagePayloadFactory;
    }

    public void sendChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildChildTurnsAgeNotificationEmailPayload(paymentCycle,
                entitlement, multipleChildrenTurningFourInNextMonth, EmailType.CHILD_TURNS_FOUR);
        log.debug("Sending email for child turns 4 for Payment Cycle after cycle with id: [{}], with a delay of {}",
                paymentCycle.getId(), changeInPaymentEmailDelay);
        messageQueueClient.sendMessageWithDelay(messagePayload, MessageType.SEND_EMAIL, changeInPaymentEmailDelay);
    }

    public void sendChildTurnsOneEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningOneInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningOne();
        EmailMessagePayload messagePayload = buildChildTurnsAgeNotificationEmailPayload(paymentCycle,
                entitlement, multipleChildrenTurningOneInNextMonth, EmailType.CHILD_TURNS_ONE);
        log.debug("Sending email for child turns 1 for Payment Cycle after cycle with id: [{}], with a delay of {}",
                paymentCycle.getId(), changeInPaymentEmailDelay);
        messageQueueClient.sendMessageWithDelay(messagePayload, MessageType.SEND_EMAIL, changeInPaymentEmailDelay);
    }

    public void sendPaymentStoppingYoungestChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildPaymentStoppingNotificationEmailPayload(paymentCycle,
                multipleChildrenTurningFourInNextMonth);
        log.debug("Sending email for payment stopping when youngest child turns 4 for Payment Cycle after cycle with id: [{}], with a delay of {}",
                paymentCycle.getId(), changeInPaymentEmailDelay);
        messageQueueClient.sendMessageWithDelay(messagePayload, MessageType.SEND_EMAIL, changeInPaymentEmailDelay);
    }

    private EmailMessagePayload buildChildTurnsAgeNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                           PaymentCycleVoucherEntitlement entitlementNextMonth,
                                                                           boolean multipleChildrenTurningAgeInMonth,
                                                                           EmailType emailType) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationMapForNextCycle(paymentCycle, entitlementNextMonth);
        emailPersonalisation.put(MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningAgeInMonth);
        return buildEmailMessagePayload(paymentCycle, emailType, emailPersonalisation);
    }

    private EmailMessagePayload buildPaymentStoppingNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                           boolean multipleChildrenTurningAgeInMonth) {
        Map<String, Object> emailPersonalisation = new HashMap<>();
        String paymentAmount = convertPenceToPounds(paymentCycle.getVoucherEntitlement().getTotalVoucherValueInPence());
        emailPersonalisation.put(PAYMENT_AMOUNT.getTemplateKeyName(), paymentAmount);
        emailPersonalisation.put(MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningAgeInMonth);
        return buildEmailMessagePayload(paymentCycle, EmailType.PAYMENT_STOPPING, emailPersonalisation);
    }

    private EmailMessagePayload buildEmailMessagePayload(PaymentCycle paymentCycle, EmailType emailType, Map<String, Object> emailPersonalisation) {
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(emailType)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

    private Map<String, Object> createEmailPersonalisationMapForNextCycle(PaymentCycle paymentCycle, PaymentCycleVoucherEntitlement voucherEntitlement) {
        Map<String, Object> emailPersonalisation = emailMessagePayloadFactory.createCommonEmailPersonalisationMap(paymentCycle, voucherEntitlement);
        emailPersonalisation.put(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName(), buildUnder1PaymentSummaryForNextCycle(voucherEntitlement));
        emailPersonalisation.put(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName(), buildUnder4PaymentSummaryForNextCycle(voucherEntitlement));
        return emailPersonalisation;
    }

    private String buildUnder4PaymentSummaryForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int vouchersForBetweenOneAndFourForFinalWeek = voucherEntitlement.getLastVoucherEntitlementForCycle().getVouchersForChildrenBetweenOneAndFour();
        int totalVouchersForChildrenUnderOne = vouchersForBetweenOneAndFourForFinalWeek * numberOfCalculationPeriods;
        return formatPaymentAmountSummary(
                "\n* %s for children between 1 and 4",
                totalVouchersForChildrenUnderOne,
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private String buildUnder1PaymentSummaryForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int vouchersForUnderOneForFinalWeek = voucherEntitlement.getLastVoucherEntitlementForCycle().getVouchersForChildrenUnderOne();
        int totalVouchersForChildrenUnderOne = vouchersForUnderOneForFinalWeek * numberOfCalculationPeriods;
        return formatPaymentAmountSummary(
                "\n* %s for children under 1",
                totalVouchersForChildrenUnderOne,
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private PaymentCycleVoucherEntitlement determineEntitlementForNextCycle(PaymentCycle currentPaymentCycle) {
        LocalDate nextCycleStartDate = currentPaymentCycle.getCycleEndDate().plusDays(1);
        return paymentCycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(currentPaymentCycle.getClaim().getClaimant().getExpectedDeliveryDate()),
                currentPaymentCycle.getChildrenDob(),
                nextCycleStartDate,
                currentPaymentCycle.getVoucherEntitlement());
    }

}
