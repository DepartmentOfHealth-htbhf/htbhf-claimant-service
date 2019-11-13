package uk.gov.dhsc.htbhf.claimant.message.payload;

/**
 * Enum containing all the types of email we currently have setup, each one should have its templateId
 * as stated in the Notify template.
 */
public enum EmailType {
    INSTANT_SUCCESS("bbbd8805-b020-41c9-b43f-c0e62318a6d5"),
    PAYMENT("9a61639f-8330-498a-8c53-31809b3837c1"),
    CHILD_TURNS_FOUR("7060a1ac-32cd-4227-8217-394c73bb712c"),
    CHILD_TURNS_ONE("a1a46ff3-3371-449e-bad6-571ab4e20286"),
    NEW_CHILD_FROM_PREGNANCY("1ed5d338-828e-428b-b537-3b9e285fd947"),
    CLAIM_NO_LONGER_ELIGIBLE("2c69f4ae-3f42-4e71-b305-247c23affb44"),
    NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE("8be94eb0-5cd0-4191-8b99-9190f80bcc53"),
    CARD_IS_ABOUT_TO_BE_CANCELLED("c4818a9f-564d-40de-adbf-c8b1a6594d75"),
    REPORT_A_BIRTH_REMINDER("0c3e273a-3cc0-4420-a7e6-187fe678ee7f"),
    // TODO HTBHF-1296 add template ID when it exists in Notify
    CLAIM_CLOSED("TODO");

    private String templateId;

    EmailType(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }
}
