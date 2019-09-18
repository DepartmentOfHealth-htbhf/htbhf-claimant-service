package uk.gov.dhsc.htbhf.claimant.message.payload;

/**
 * Enum containing all the types of email we currently have setup, each one should have a templateId setup in the config.
 */
public enum EmailType {
    NEW_CARD,
    PAYMENT,
    CHILD_TURNS_FOUR
}
