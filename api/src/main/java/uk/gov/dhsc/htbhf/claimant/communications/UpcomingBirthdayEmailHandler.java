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
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
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
@SuppressWarnings("PMD.TooManyMethods")
public class UpcomingBirthdayEmailHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Integer numberOfCalculationPeriods;
    private final MessageQueueClient messageQueueClient;
    private final ChildDateOfBirthCalculator childDateOfBirthCalculator;
    private final PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    public UpcomingBirthdayEmailHandler(@Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods,
                                        MessageQueueClient messageQueueClient,
                                        ChildDateOfBirthCalculator childDateOfBirthCalculator,
                                        PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
        this.messageQueueClient = messageQueueClient;
        this.childDateOfBirthCalculator = childDateOfBirthCalculator;
        this.paymentCycleEntitlementCalculator = paymentCycleEntitlementCalculator;
    }

    public void handleUpcomingBirthdayEmails(PaymentCycle paymentCycle) {
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        if (nextPaymentCycleSummary.hasChildrenTurningFour()) {
            sendChildTurnsFourEmail(paymentCycle, nextPaymentCycleSummary);
        }

        if (nextPaymentCycleSummary.hasChildrenTurningOne()) {
            sendChildTurnsOneEmail(paymentCycle, nextPaymentCycleSummary);
        }
    }

    private void sendChildTurnsFourEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningFourInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningFour();
        EmailMessagePayload messagePayload = buildChildTurnsFourNotificationEmailPayload(paymentCycle, entitlement, multipleChildrenTurningFourInNextMonth);
        log.info("Sending email for child turns 4 for Payment Cycle after cycle with id: [{}]", paymentCycle.getId());
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    private void sendChildTurnsOneEmail(PaymentCycle paymentCycle, NextPaymentCycleSummary dateOfBirthSummaryAffectingNextPayment) {
        PaymentCycleVoucherEntitlement entitlement = determineEntitlementForNextCycle(paymentCycle);
        boolean multipleChildrenTurningOneInNextMonth = dateOfBirthSummaryAffectingNextPayment.hasMultipleChildrenTurningOne();
        EmailMessagePayload messagePayload = buildChildTurnsOneNotificationEmailPayload(paymentCycle, entitlement, multipleChildrenTurningOneInNextMonth);
        log.info("Sending email for child turns 1 for Payment Cycle after cycle with id: [{}]", paymentCycle.getId());
        messageQueueClient.sendMessage(messagePayload, MessageType.SEND_EMAIL);
    }

    private EmailMessagePayload buildChildTurnsFourNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                            PaymentCycleVoucherEntitlement entitlementNextMonth,
                                                                            boolean multipleChildrenTurningFourInMonth) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationMapForNextCycle(paymentCycle, entitlementNextMonth);
        emailPersonalisation.put(MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningFourInMonth);
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(EmailType.CHILD_TURNS_FOUR)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

    private EmailMessagePayload buildChildTurnsOneNotificationEmailPayload(PaymentCycle paymentCycle,
                                                                           PaymentCycleVoucherEntitlement entitlementNextMonth,
                                                                           boolean multipleChildrenTurningOneInMonth) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationMapForNextCycle(paymentCycle, entitlementNextMonth);
        emailPersonalisation.put(MULTIPLE_CHILDREN.getTemplateKeyName(), multipleChildrenTurningOneInMonth);
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(EmailType.CHILD_TURNS_ONE)
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
