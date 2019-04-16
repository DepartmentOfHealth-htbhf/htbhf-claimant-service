package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.time.LocalDate;

public class TestConstants {

    public static final String DWP_HOUSEHOLD_IDENTIFIER = "dwpHousehold1";
    public static final String HMRC_HOUSEHOLD_IDENTIFIER = "hmrcHousehold1";

    public static final int NUMBER_OF_CHILDREN_UNDER_ONE = 1;
    public static final int NUMBER_OF_CHILDREN_UNDER_FOUR = 2;

    public static final int VOUCHER_VALUE_IN_PENCE = 310;
    public static final int VOUCHERS_FOR_CHILDREN_UNDER_ONE = 2;
    public static final int VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR = 1;
    public static final int VOUCHERS_FOR_PREGNANCY = 1;
    public static final int TOTAL_VOUCHER_ENTITLEMENT = 4;
    public static final int TOTAL_VOUCHER_VALUE_IN_PENCE = 1240;

    public static final LocalDate JAMES_DATE_OF_BIRTH = LocalDate.parse("1985-12-31");
    public static final String JAMES_FIRST_NAME = "James";
    public static final String JAMES_LAST_NAME = "Smith";
}
