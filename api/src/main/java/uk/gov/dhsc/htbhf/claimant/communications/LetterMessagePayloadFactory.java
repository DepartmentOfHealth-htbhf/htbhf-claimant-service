package uk.gov.dhsc.htbhf.claimant.communications;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.message.LetterTemplateKey.*;

/**
 * Builds the message payload required to send a letter message. The letter template has parameterised values
 * which are contained in the personalisation Map. The only required args are the first two lines of an address and the postcode,
 * though we always populate 5 lines of address.
 */
@Component
public class LetterMessagePayloadFactory {

    public static LetterMessagePayload buildLetterPayloadWithAddressOnly(Claim claim, LetterType letterType) {
        Map<String, Object> personalisationMap = createAddressPersonalisationMap(claim.getClaimant());

        return LetterMessagePayload.builder()
                .claimId(claim.getId())
                .letterType(letterType)
                .personalisation(personalisationMap)
                .build();
    }

    public static LetterMessagePayload buildLetterPayloadWithAddressAndPaymentFields(Claim claim,
                                                                                     EligibilityAndEntitlementDecision decision,
                                                                                     LetterType letterType) {
        Map<String, Object> personalisationMap = new HashMap<>();
        personalisationMap.putAll(createAddressPersonalisationMap(claim.getClaimant()));
        personalisationMap.putAll(createPaymentPersonalisationMap(decision.getVoucherEntitlement()));

        return LetterMessagePayload.builder()
                .claimId(claim.getId())
                .letterType(letterType)
                .personalisation(personalisationMap)
                .build();
    }

    private static Map<String, Object> createAddressPersonalisationMap(Claimant claimant) {
        Address address = claimant.getAddress();
        Map<String, Object> personalisationMap = new HashMap<>();
        personalisationMap.put(ADDRESS_LINE_1.getTemplateKeyName(), claimant.getFirstName() + " " + claimant.getLastName());
        personalisationMap.put(ADDRESS_LINE_2.getTemplateKeyName(), address.getAddressLine1());
        personalisationMap.put(ADDRESS_LINE_3.getTemplateKeyName(), address.getAddressLine2());
        personalisationMap.put(ADDRESS_LINE_4.getTemplateKeyName(), address.getTownOrCity());
        personalisationMap.put(ADDRESS_LINE_5.getTemplateKeyName(), address.getCounty());
        personalisationMap.put(POSTCODE.getTemplateKeyName(), address.getPostcode());
        return personalisationMap;
    }

    private static Map<String, Object> createPaymentPersonalisationMap(PaymentCycleVoucherEntitlement voucherEntitlement) {
        int singleVoucherValueInPence = voucherEntitlement.getSingleVoucherValueInPence();
        return Map.of(
                PAYMENT_AMOUNT.getTemplateKeyName(), voucherEntitlement.getTotalVoucherValueInPence(),
                PREGNANCY_AMOUNT.getTemplateKeyName(), voucherEntitlement.getVouchersForPregnancy() * singleVoucherValueInPence,
                CHILDREN_UNDER_1_PAYMENT.getTemplateKeyName(), voucherEntitlement.getVouchersForChildrenUnderOne() * singleVoucherValueInPence,
                CHILDREN_UNDER_4_PAYMENT.getTemplateKeyName(), voucherEntitlement.getVouchersForChildrenBetweenOneAndFour() * singleVoucherValueInPence
        );
    }

}
