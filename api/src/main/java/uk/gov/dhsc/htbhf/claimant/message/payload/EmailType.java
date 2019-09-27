package uk.gov.dhsc.htbhf.claimant.message.payload;

/**
 * Enum containing all the types of email we currently have setup, each one should have its templateId
 * as stated in the Notify template.
 */
public enum EmailType {
    NEW_CARD("bbbd8805-b020-41c9-b43f-c0e62318a6d5"),
    PAYMENT("9a61639f-8330-498a-8c53-31809b3837c1"),
    CHILD_TURNS_FOUR("7060a1ac-32cd-4227-8217-394c73bb712c"),
    CHILD_TURNS_ONE("a1a46ff3-3371-449e-bad6-571ab4e20286"),
    CLAIM_NO_LONGER_ELIGIBLE("2c69f4ae-3f42-4e71-b305-247c23affb44");

    private String templateId;

    EmailType(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }
}
