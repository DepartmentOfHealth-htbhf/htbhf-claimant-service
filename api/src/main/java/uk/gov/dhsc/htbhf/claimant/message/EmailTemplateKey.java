package uk.gov.dhsc.htbhf.claimant.message;

/**
 * Enum containing the keys that are used for parameterised values within Notify email templates.
 */
public enum EmailTemplateKey {

    FIRST_NAME("First_name"),
    LAST_NAME("Last_name"),
    FIRST_PAYMENT_AMOUNT("first_payment_amount"),
    PAYMENT_AMOUNT("payment_amount"),
    PREGNANCY_PAYMENT("pregnancy_payment"),
    CHILDREN_UNDER_1_PAYMENT("children_under_1_payment"),
    CHILDREN_UNDER_4_PAYMENT("children_under_4_payment"),
    NEXT_PAYMENT_DATE("next_payment_date"),
    MULTIPLE_CHILDREN("multiple_children");

    private String templateKeyName;

    EmailTemplateKey(String templateKeyName) {
        this.templateKeyName = templateKeyName;
    }

    public String getTemplateKeyName() {
        return templateKeyName;
    }
}
