package uk.gov.dhsc.htbhf.claimant;

import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.util.Map;

/**
 * Contains all the mappings between email types and template ids. This is built on startup
 * from configuration only.
 */
public class EmailTemplateConfig {

    private final Map<EmailType, String> templateIds;

    public EmailTemplateConfig(Map<EmailType, String> templateIds) {
        this.templateIds = templateIds;
    }

    /**
     * Get the templateId for the given email type.
     *
     * @param emailType The type of email to get the template id for
     * @return The template id
     */
    public String getTemplateIdForEmail(EmailType emailType) {
        if (!templateIds.containsKey(emailType)) {
            throw new IllegalArgumentException("No template id defined for email type: " + emailType);
        }
        return templateIds.get(emailType);
    }
}
