package uk.gov.dhsc.htbhf.claimant.communications;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextType;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.message.MoneyUtils.convertPenceToPounds;
import static uk.gov.dhsc.htbhf.claimant.message.TextTemplateKey.PAYMENT_AMOUNT;
import static uk.gov.dhsc.htbhf.claimant.message.TextTemplateKey.REFERENCE_NUMBER;

/**
 * Builds the message payload required to send an text message. The text template has parameterised values
 * which are contained in the textPersonalisation Map. All monetary amounts are formatted into pounds.
 */
@Component
public class TextMessagePayloadFactory {

    public TextMessagePayload buildTextMessagePayload(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement, TextType textType) {
        Map<String, Object> textPersonalisation = createPaymentTextPersonalisationMap(voucherEntitlement, claim);
        return TextMessagePayload.builder()
                .claimId(claim.getId())
                .textType(textType)
                .textPersonalisation(textPersonalisation)
                .build();
    }

    private Map<String, Object> createPaymentTextPersonalisationMap(PaymentCycleVoucherEntitlement voucherEntitlement, Claim claim) {
        Map<String, Object> textPersonalisation = new HashMap<>();
        String paymentAmount = convertPenceToPounds(voucherEntitlement.getTotalVoucherValueInPence());
        textPersonalisation.put(PAYMENT_AMOUNT.getTemplateKeyName(), paymentAmount);
        textPersonalisation.put(REFERENCE_NUMBER.getTemplateKeyName(), claim.getReference());

        return textPersonalisation;
    }
}
