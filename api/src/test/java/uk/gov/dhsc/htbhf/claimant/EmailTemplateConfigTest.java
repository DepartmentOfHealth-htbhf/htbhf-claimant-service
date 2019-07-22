package uk.gov.dhsc.htbhf.claimant;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class EmailTemplateConfigTest {

    private static final String TEMPLATE_ID1 = "TemplateId1";
    private static final Map<EmailType, String> TEMPLATE_IDS = Map.of(EmailType.NEW_CARD, TEMPLATE_ID1);
    private EmailTemplateConfig emailTemplateConfig = new EmailTemplateConfig(TEMPLATE_IDS);

    @Test
    void shouldAddTemplateId() {
        //Given
        //When
        String templateId = emailTemplateConfig.getTemplateIdForEmail(EmailType.NEW_CARD);
        //Then
        assertThat(templateId).isEqualTo(TEMPLATE_ID1);
    }

    @Test
    void shouldFailToFindTemplateIdIfNotStored() {
        //Given no templateIds stored
        //When
        IllegalArgumentException caught = catchThrowableOfType(
                () -> emailTemplateConfig.getTemplateIdForEmail(EmailType.PAYMENT),
                IllegalArgumentException.class);
        //Then
        assertThat(caught).hasMessage("No template id defined for email type: PAYMENT");
    }

}
