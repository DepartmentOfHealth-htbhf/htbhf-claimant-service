package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_LAST_NAME;

public class EmailPersonalisationMapTestDataFactory {

    /**
     * Builds test email personalisation for email parameterisation.
     *
     * @return The Map of email parameters and their values for testing.
     */
    public static Map<String, Object> buildEmailPersonalisation() {
        return Map.of("First_name", VALID_FIRST_NAME,
                "Last_name", VALID_LAST_NAME,
                "first_payment_amount", "£13.60",
                "pregnancy_payment", "\n* £3.40 for a pregnancy",
                "children_under_1_payment", "",
                "children_under_4_payment", "\n* £6.80 for 2 children between 1 and 3 years old",
                "next_payment_date", "27 July 2019");
    }

}
