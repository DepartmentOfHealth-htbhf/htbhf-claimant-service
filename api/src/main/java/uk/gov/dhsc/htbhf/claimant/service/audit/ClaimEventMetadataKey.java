package uk.gov.dhsc.htbhf.claimant.service.audit;

/**
 * Enum storing the metadata keys used for Claim Events. The keys are stored here to make sure that we have
 * consistency in metadata keys between different events.
 */
public enum ClaimEventMetadataKey {
    CLAIM_ID("claimId"),
    CLAIM_STATUS("claimStatus"),
    ELIGIBILITY_STATUS("eligibilityStatus"),
    CARD_ACCOUNT_ID("cardAccountId");

    private String key;

    ClaimEventMetadataKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
