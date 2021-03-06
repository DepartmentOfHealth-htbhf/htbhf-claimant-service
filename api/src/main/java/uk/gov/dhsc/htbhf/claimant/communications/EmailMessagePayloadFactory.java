package uk.gov.dhsc.htbhf.claimant.communications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildPregnancyPaymentAmountSummary;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildUnder1PaymentSummary;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.buildUnder4PaymentSummary;
import static uk.gov.dhsc.htbhf.claimant.communications.MessagePayloadUtils.formatRequiredPaymentAmount;
import static uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey.*;
import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;

/**
 * Builds the message payload required to send an email message. The email template has parameterised values
 * which are contained in the emailPersonalisation Map. All monetary amounts are formatted into pounds and the breakdown
 * of voucher payments has been detailed in a bullet point list - any vouchers which are missing are replaced with a
 * single blank line so that we don't have any empty bullet point in the email.
 */
@Component
@SuppressWarnings("PMD.TooManyMethods")
public class EmailMessagePayloadFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Integer numberOfCalculationPeriods;

    public static EmailMessagePayload buildEmailMessagePayloadWithFirstAndLastNameOnly(Claim claim, EmailType emailType) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationWithFirstAndLastNameOnly(claim.getClaimant());

        return EmailMessagePayload.builder()
                .emailPersonalisation(emailPersonalisation)
                .emailType(emailType)
                .claimId(claim.getId())
                .build();
    }

    public static Map<String, Object> createEmailPersonalisationWithFirstAndLastNameOnly(Claimant claimant) {
        return Map.of(
                FIRST_NAME.getTemplateKeyName(), claimant.getFirstName(),
                LAST_NAME.getTemplateKeyName(), claimant.getLastName());
    }

    public EmailMessagePayloadFactory(@Value("${payment-cycle.number-of-calculation-periods}") Integer numberOfCalculationPeriods) {
        this.numberOfCalculationPeriods = numberOfCalculationPeriods;
    }

    public EmailMessagePayload buildEmailMessagePayload(PaymentCycle paymentCycle, EmailType emailType) {
        Claim claim = paymentCycle.getClaim();
        LocalDate nextPaymentDate = paymentCycle.getCycleEndDate().plusDays(1);
        PaymentCycleVoucherEntitlement voucherEntitlement = paymentCycle.getVoucherEntitlement();
        return buildEmailMessagePayload(claim, voucherEntitlement, nextPaymentDate, emailType);
    }

    public EmailMessagePayload buildEmailMessagePayload(
            Claim claim,
            PaymentCycleVoucherEntitlement voucherEntitlement,
            LocalDate nextPaymentDate,
            EmailType emailType) {
        Map<String, Object> emailPersonalisation = createPaymentEmailPersonalisationMap(claim.getClaimant(), voucherEntitlement, nextPaymentDate);
        return EmailMessagePayload.builder()
                .claimId(claim.getId())
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
        LocalDate nextPaymentDate = paymentCycle.getCycleEndDate().plusDays(1);
        return createCommonEmailPersonalisationMap(claimant, voucherEntitlement, nextPaymentDate);
    }

    private Map<String, Object> createCommonEmailPersonalisationMap(
            Claimant claimant,
            PaymentCycleVoucherEntitlement voucherEntitlement,
            LocalDate nextPaymentDate) {
        Map<String, Object> emailPersonalisation = new HashMap<>();
        emailPersonalisation.put(FIRST_NAME.getTemplateKeyName(), claimant.getFirstName());
        emailPersonalisation.put(LAST_NAME.getTemplateKeyName(), claimant.getLastName());
        String paymentAmount = convertPenceToPounds(voucherEntitlement.getTotalVoucherValueInPence());
        emailPersonalisation.put(PAYMENT_AMOUNT.getTemplateKeyName(), paymentAmount);
        String formattedNextPaymentDate = nextPaymentDate.format(DATE_FORMATTER);
        emailPersonalisation.put(NEXT_PAYMENT_DATE.getTemplateKeyName(), formattedNextPaymentDate);
        emailPersonalisation.put(REGULAR_PAYMENT.getTemplateKeyName(), getRegularPaymentAmountForNextCycle(voucherEntitlement));
        emailPersonalisation.put(PREGNANCY_PAYMENT.getTemplateKeyName(), buildPregnancyPaymentAmountSummary(voucherEntitlement));
        return emailPersonalisation;
    }

    private Map<String, Object> createPaymentEmailPersonalisationMap(
            Claimant claimant,
            PaymentCycleVoucherEntitlement voucherEntitlement,
            LocalDate nextPaymentDate) {
        Map<String, Object> emailPersonalisation = createCommonEmailPersonalisationMap(claimant, voucherEntitlement, nextPaymentDate);
        emailPersonalisation.put(CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName(), buildUnder1PaymentSummary(voucherEntitlement));
        emailPersonalisation.put(CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName(), buildUnder4PaymentSummary(voucherEntitlement));
        String backdateAmount = convertPenceToPounds(voucherEntitlement.getBackdatedVouchersValueInPence());
        emailPersonalisation.put(BACKDATED_AMOUNT.getTemplateKeyName(), backdateAmount);
        return emailPersonalisation;
    }

    private String getRegularPaymentAmountForNextCycle(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int totalVouchersForRegularPayment = voucherEntitlement.getLastVoucherEntitlementForCycle().getTotalVoucherEntitlement() * numberOfCalculationPeriods;
        return formatRequiredPaymentAmount("%s", totalVouchersForRegularPayment, voucherEntitlement.getSingleVoucherValueInPence());
    }
}
