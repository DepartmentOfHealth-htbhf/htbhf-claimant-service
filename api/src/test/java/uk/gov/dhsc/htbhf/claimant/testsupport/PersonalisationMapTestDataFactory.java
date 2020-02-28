package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.util.Map;

import static uk.gov.dhsc.htbhf.TestConstants.*;

public class PersonalisationMapTestDataFactory {

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

    /**
     * Builds test text personalisation for text parameterisation.
     *
     * @return The Map of text parameters and their values for testing.
     */
    public static Map<String, Object> buildTextPersonalisation() {
        return Map.of(
                "payment_amount", "£13.60",
                "reference_number", "0E1567C0B2");
    }

    /**
     * Builds test personalisation for letter parameterisation.
     *
     * @return The Map of parameters and their values for testing.
     */
    public static Map<String, Object> buildLetterPersonalisation() {
        return Map.of("address_line_1", HOMER_FORENAME + " " + SIMPSON_SURNAME,
                "address_line_2", SIMPSONS_ADDRESS_LINE_1,
                "address_line_3", SIMPSONS_ADDRESS_LINE_2,
                "address_line_4", SIMPSONS_TOWN,
                "address_line_5", SIMPSONS_COUNTY,
                "postcode", SIMPSONS_POSTCODE);
    }

}
