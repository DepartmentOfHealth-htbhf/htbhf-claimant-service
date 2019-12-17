package uk.gov.dhsc.htbhf.claimant.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LetterTemplateKey {

    ADDRESS_LINE_1("address_line_1"),
    ADDRESS_LINE_2("address_line_2"),
    ADDRESS_LINE_3("address_line_3"),
    ADDRESS_LINE_4("address_line_4"),
    ADDRESS_LINE_5("address_line_5"),
    ADDRESS_LINE_6("address_line_6"),
    POSTCODE("postcode");

    private final String templateKeyName;

}
