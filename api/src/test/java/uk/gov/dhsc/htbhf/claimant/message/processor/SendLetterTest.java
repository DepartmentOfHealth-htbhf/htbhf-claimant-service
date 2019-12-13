package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.extern.slf4j.Slf4j;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendLetterResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class SendLetterTest {

    private static final String UPDATE_ADDRESS_TEMPLATE_ID = "518e6e56-32db-4a8c-ad7a-7baa99777f79";

    /**
     * Created as a main method to test out a single letter being sent
     * TODO MRS 12/12/2019: HTBHF-2740 Remove this code and use the basis of it to send a letter in the address mismatch scenario.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Should have a single argument which is the Notify API key");
        }
        String notifyApiKey = args[0];
        NotificationClient client = new NotificationClient(notifyApiKey);
        String reference = UUID.randomUUID().toString();
        try {
            Map<String, Object> personalisation = new HashMap<>();
            personalisation.put("address_line_1", "Gethyn Jones");
            personalisation.put("address_line_2", "Equal Experts");
            personalisation.put("address_line_3", "Desklodge House");
            personalisation.put("address_line_4", "Redcliff Way");
            personalisation.put("address_line_5", "Bristol");
            personalisation.put("postcode", "BS1 6NL");
            SendLetterResponse response = client.sendLetter(
                    UPDATE_ADDRESS_TEMPLATE_ID,
                    personalisation,
                    reference
            );
            log.debug("Letter sent, reference={}, response={}", reference, response);
        } catch (NotificationClientException e) {
            log.error("Failed to send letter request", e);
        }
    }

}
