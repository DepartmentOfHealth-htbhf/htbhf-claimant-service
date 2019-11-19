package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.util.Map;

import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_FORENAME;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.SIMPSON_SURNAME;

public class EmailPersonalisationMapTestDataFactory {

    /**
     * Builds test email personalisation for email parameterisation.
     *
     * @return The Map of email parameters and their values for testing.
     */
    public static Map<String, Object> buildEmailPersonalisation() {
        return Map.of("First_name", HOMER_FORENAME,
                "Last_name", SIMPSON_SURNAME,
                "first_payment_amount", "£13.60",
                "pregnancy_payment", "\n* £3.40 for a pregnancy",
                "children_under_1_payment", "",
                "children_under_4_payment", "\n* £6.80 for 2 children between 1 and 3 years old",
                "next_payment_date", "27 July 2019");
    }

}
