package uk.gov.dhsc.htbhf.claimant.regex;

public class PostcodeRegex {
    /**
     * Regex for matching UK postcodes matching BS7666 format.
     * { @see https://www.gov.uk/government/publications/bulk-data-transfer-for-sponsors-xml-schema } The format is in the file BulkDataCommon-v2.1.xsd
     * { @see https://stackoverflow.com/questions/164979/uk-postcode-regex-comprehensive }
     */
    public static final String UK_POST_CODE_REGEX = "([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})"
            + "|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9][A-Za-z]?))))\\s?[0-9][A-Za-z]{2})";

    /**
     * Regex for matching UK Channel Island and Isle of Man postcodes.
     */
    public static final String CHANNEL_ISLAND_POST_CODE_REGEX = "([Gg][Yy]|[Jj][Ee]|[Ii][Mm]).+";
}
