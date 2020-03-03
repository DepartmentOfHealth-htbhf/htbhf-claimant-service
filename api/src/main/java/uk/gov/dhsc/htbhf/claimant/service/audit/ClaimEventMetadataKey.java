package uk.gov.dhsc.htbhf.claimant.service.audit;

/**
 * Enum storing the metadata keys used for Claim Events. The keys are stored here to make sure that we have
 * consistency in metadata keys between different events.
 */
public enum ClaimEventMetadataKey {
    CLAIM_ID("claimId"),
    CLAIM_STATUS("claimStatus"),
    ELIGIBILITY_STATUS("eligibilityStatus"),
    CARD_ACCOUNT_ID("cardAccountId"),
    PAYMENT_ID("paymentId"),
    PAYMENT_REQUEST_REFERENCE("paymentRequestReference"),
    PAYMENT_RESPONSE_REFERENCE("paymentResponseReference"),
    PAYMENT_AMOUNT("paymentAmount"),
    ENTITLEMENT_AMOUNT_IN_PENCE("entitlementAmountInPence"),
    BALANCE_ON_CARD("balanceOnCard"),
    UPDATED_FIELDS("updatedFields"),
    EMAIL_TYPE("emailType"),
    TEXT_TYPE("textType"),
    LETTER_TYPE("letterType"),
    TEMPLATE_ID("templateId"),
    USER("user");

    private String key;

    ClaimEventMetadataKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
