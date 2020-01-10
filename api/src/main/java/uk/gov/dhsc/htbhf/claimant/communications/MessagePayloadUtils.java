package uk.gov.dhsc.htbhf.claimant.communications;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

/**
 * Utility class for creating email and letter message payloads.
 */
public class MessagePayloadUtils {

    public static String formatPaymentAmountSummary(String summaryTemplate, int numberOfVouchers, int voucherAmountInPence) {
        return formatOptionalPaymentAmount(summaryTemplate, numberOfVouchers, voucherAmountInPence);
    }

    public static String formatOptionalPaymentAmount(String template, int numberOfVouchers, int voucherAmountInPence) {
        if (numberOfVouchers == 0) {
            return "";
        }
        return formatRequiredPaymentAmount(template, numberOfVouchers, voucherAmountInPence);
    }

    public static String formatRequiredPaymentAmount(String template, int numberOfVouchers, int voucherAmountInPence) {
        int totalAmount = numberOfVouchers * voucherAmountInPence;
        return String.format(template, convertPenceToPounds(totalAmount));
    }

    public static String buildPregnancyPaymentAmountSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for a pregnancy",
                voucherEntitlement.getVouchersForPregnancy(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    public static String buildUnder1PaymentSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for children under 1",
                voucherEntitlement.getVouchersForChildrenUnderOne(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }

    public static String buildUnder4PaymentSummary(PaymentCycleVoucherEntitlement voucherEntitlement) {
        return formatPaymentAmountSummary(
                "\n* %s for children between 1 and 4",
                voucherEntitlement.getVouchersForChildrenBetweenOneAndFour(),
                voucherEntitlement.getSingleVoucherValueInPence());
    }
}
