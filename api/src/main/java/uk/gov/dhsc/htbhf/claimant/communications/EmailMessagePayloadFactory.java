package uk.gov.dhsc.htbhf.claimant.communications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

/**
 * Builds the message payload required to send an email message. The email template has parameterised values
 * which are contained in the emailPersonalisation Map. All monetary amounts are formatted into pounds and the breakdown
 * of voucher payments has been detailed in a bullet point list - any vouchers which are missing are replaced with a
 * single blank line so that we don't have any empty bullet point in the email.
 */
@Component
public class EmailMessagePayloadFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Integer numberOfCalculationPeriods;

    public static String formatPaymentAmountSummary(String summaryTemplate, int numberOfVouchers, int voucherAmountInPence) {
        if (numberOfVouchers == 0) {
            return "";
        }
        int totalAmount = numberOfVouchers * voucherAmountInPence;
        return String.format(summaryTemplate, convertPenceToPounds(totalAmount));
    }

    public static String formatRegularPaymentAmount(int numberOfVouchers, int voucherAmountInPence) {
        if (numberOfVouchers == 0) {
            return "";
        }
        int totalAmount = numberOfVouchers * voucherAmountInPence;
        return convertPenceToPounds(totalAmount);
    }

    public EmailMessagePayloadFactory(@Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
    }

    public EmailMessagePayload buildEmailMessagePayload(PaymentCycle paymentCycle, EmailType emailType) {
        Map<String, Object> emailPersonalisation = createPaymentEmailPersonalisationMap(paymentCycle, paymentCycle.getVoucherEntitlement());
        return EmailMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .emailType(emailType)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

    /**
     * Creates an email personalisation map with common fields: first_name, last_name, payment_amount, next_payment_date, regular_payment and pregnancy_amount.
     * @param paymentCycle the current payment cycle
     * @param voucherEntitlement the voucher entitlement the email is referring to. Either the current or next cycle
     * @return email personalisation map with common fields
     */
    public Map<String, Object> createCommonEmailPersonalisationMap(PaymentCycle paymentCycle, PaymentCycleVoucherEntitlement voucherEntitlement) {
        Claimant claimant = paymentCycle.getClaim().getClaimant();
        Map<String, Object> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put(FIRST_NAME.getTemplateKeyName(), claimant.getFirstName());
        emailPersonalisation.put(LAST_NAME.getTemplateKeyName(), claimant.getLastName());
        String paymentAmount = convertPenceToPounds(voucherEntitlement.getTotalVoucherValueInPence());
        emailPersonalisation.put(PAYMENT_AMOUNT.getTemplateKeyName(), paymentAmount);
        String formattedCycleEndDate = paymentCycle.getCycleEndDate().plusDays(1).format(DATE_FORMATTER);
        emailPersonalisation.put(NEXT_PAYMENT_DATE.getTemplateKeyName(), formattedCycleEndDate);
        emailPersonalisation.put(REGULAR_PAYMENT.getTemplateKeyName(), getRegularPaymentAmountForNextCycle(voucherEntitlement));
        emailPersonalisation.put(PREGNANCY_PAYMENT.getTemplateKeyName(), buildPregnancyPaymentAmountSummary(voucherEntitlement));
        return emailPersonalisation;
    }

    private Map<String, Object> createPaymentEmailPersonalisationMap(PaymentCycle paymentCycle, PaymentCycleVoucherEntitlement voucherEntitlement) {
        Map<String, Object> emailPersonalisation = createCommonEmailPersonalisationMap(paymentCycle, voucherEntitlement);
        emailPersonalisation.put(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName(), buildUnder1PaymentSummary(voucherEntitlement));
        emailPersonalisation.put(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName(), buildUnder4PaymentSummary(voucherEntitlement));
        String backdateAmount = convertPenceToPounds(voucherEntitlement.getBackdatedVouchersValueInPence());
        emailPersonalisation.put(BACKDATED_AMOUNT.getTemplateKeyName(), backdateAmount);
        return emailPersonalisation;
    }

    private static String buildPregnancyPaymentAmountSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for a pregnancy",
                voucherEntitlement.getVouchersForPregnancy(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private static String buildUnder1PaymentSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for children under 1",
                voucherEntitlement.getVouchersForChildrenUnderOne(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private static String buildUnder4PaymentSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for children between 1 and 4",
                voucherEntitlement.getVouchersForChildrenBetweenOneAndFour(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    private String getRegularPaymentAmountForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int totalVouchersForRegularPayment = voucherEntitlement.getLastVoucherEntitlementForCycle().getTotalVoucherEntitlement() * numberOfCalculationPeriods;
        return formatRegularPaymentAmount(totalVouchersForRegularPayment, voucherEntitlement.getSingleVoucherValueInPence());
    }
}
