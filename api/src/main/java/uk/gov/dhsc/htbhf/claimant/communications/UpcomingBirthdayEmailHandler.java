package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.formatPaymentAmountSummary;
import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

/**
 * Component responsible for determining if any additional emails need to be sent out in addition to
 * the normal Payment email for a given PaymentCycle.
 */
@Component
@Slf4j
public class UpcomingBirthdayEmailHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Integer numberOfCalculationPeriods;
    private final MessageQueueClient messageQueueClient;
    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    public UpcomingBirthdayEmailHandler(@Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                        MessageQueueClient messageQueueClient,
                                        PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.messageQueueClient = messageQueueClient;
        this.paymentCycleEntitlementCalculator = paymentCycleEntitlementCalculator;
    }

    public void sendChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildChildTurnsAgeNotificationEmailPayload(paymentCycle,
                entitlement, multipleChildrenTurningFourInNextMonth, EmailType.CHILD_TURNS_FOUR);
        log.info("Sending email for child turns 4 for Payment Cycle after cycle with id: [{}]", paymentCycle.getId());
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    public void sendChildTurnsOneEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningOneInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningOne();
        EmailMessagePayload messagePayload = buildChildTurnsAgeNotificationEmailPayload(paymentCycle,
                entitlement, multipleChildrenTurningOneInNextMonth, EmailType.CHILD_TURNS_ONE);
        log.info("Sending email for child turns 1 for Payment Cycle after cycle with id: [{}]", paymentCycle.getId());
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    private EmailMessagePayload buildChildTurnsAgeNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                           PaymentCycleVoucherEntitlement entitlementNextMonth,
                                                                           boolean multipleChildrenTurningAgeInMonth,
                                                                           EmailType emailType) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationMapForNextCycle(paymentCycle, entitlementNextMonth);
        emailPersonalisation.put(MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningAgeInMonth);
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(emailType)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

    private Map<String, Object> createEmailPersonalisationMapForNextCycle(PaymentCycle paymentCycle, PaymentCycleVoucherEntitlement voucherEntitlement) {
        Claimant claimant = paymentCycle.getClaim().getClaimant();
        Map<String, Object> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put(FIRST_NAME.getTemplateKeyName(), claimant.getFirstName());
        emailPersonalisation.put(LAST_NAME.getTemplateKeyName(), claimant.getLastName());
        emailPersonalisation.put(PAYMENT_AMOUNT.getTemplateKeyName(), convertPenceToPounds(voucherEntitlement.getTotalVoucherValueInPence()));
        emailPersonalisation.put(PREGNANCY_PAYMENT.getTemplateKeyName(), buildPregnancyPaymentAmountSummaryForNextCycle(voucherEntitlement));
        emailPersonalisation.put(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName(), buildUnder1PaymentSummaryForNextCycle(voucherEntitlement));
        emailPersonalisation.put(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName(), buildUnder4PaymentSummaryForNextCycle(voucherEntitlement));
        emailPersonalisation.put(REGULAR_PAYMENT.getTemplateKeyName(), getRegularPaymentAmountForNextCycle(voucherEntitlement));
        String formattedCycleEndDate = paymentCycle.getCycleEndDate().plusDays(1).format(DATE_FORMATTER);
        emailPersonalisation.put(NEXT_PAYMENT_DATE.getTemplateKeyName(), formattedCycleEndDate);
        return emailPersonalisation;
    }

    private String getRegularPaymentAmountForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int totalVouchersForRegularPayment = voucherEntitlement.getLastVoucherEntitlementForCycle().getTotalVoucherEntitlement() * numberOfCalculationPeriods;
        return formatRegularPaymentAmount(totalVouchersForRegularPayment, voucherEntitlement.getSingleVoucherValueInPence());
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

    private String buildPregnancyPaymentAmountSummaryForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for a pregnancy",
                voucherEntitlement.getVouchersForPregnancy(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private String formatRegularPaymentAmount(int numberOfVouchers, int voucherAmountInPence) {
        if (numberOfVouchers == 0) {
            return "";
        }
        int totalAmount = numberOfVouchers * voucherAmountInPence;
        return convertPenceToPounds(totalAmount);
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
