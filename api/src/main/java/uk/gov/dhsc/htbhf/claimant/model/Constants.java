package uk.gov.dhsc.htbhf.claimant.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class Constants {

    // Using simplified regex due to limitations of java unicode in regular expressions.
    public static final String VALID_EMAIL_REGEX = "^[^@]+@[a-zA-Z0-9.-]+\\.[a-zA-Z0-9-]+$";

    public static final String VALID_EMAIL_REGEX_V3 = "(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$)";
}
