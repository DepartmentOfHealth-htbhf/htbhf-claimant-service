package uk.gov.dhsc.htbhf.claimant.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum containing the keys that are used for parameterised values within Notify text templates.
 */
@Getter
@RequiredArgsConstructor
public enum TextTemplateKey {

    PAYMENT_AMOUNT("payment_amount"),
    REFERENCE_NUMBER("reference_number");

    private final String templateKeyName;
}
