package uk.gov.dhsc.htbhf.claimant.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum containing the keys that are used for parameterised values within Notify email templates.
 */
@Getter
@RequiredArgsConstructor
public enum EmailTemplateKey {

    FIRST_NAME("First_name"),
    LAST_NAME("Last_name"),
    PAYMENT_AMOUNT("payment_amount"),
    PREGNANCY_PAYMENT("pregnancy_payment"),
    CHILDREN_UNDER_1_PAYMENT("children_under_1_payment"),
    CHILDREN_UNDER_4_PAYMENT("children_under_4_payment"),
    NEXT_PAYMENT_DATE("next_payment_date"),
    MULTIPLE_CHILDREN("multiple_children"),
    REGULAR_PAYMENT("regular_payment"),
    BACKDATED_AMOUNT("backdated_amount");

    private final String templateKeyName;
}
