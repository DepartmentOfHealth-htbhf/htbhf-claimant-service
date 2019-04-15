package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.math.BigDecimal;

public class TestConstants {

    public static final String DWP_HOUSEHOLD_IDENTIFIER = "dwpHousehold1";
    public static final String HMRC_HOUSEHOLD_IDENTIFIER = "hmrcHousehold1";

    public static final int NUMBER_OF_CHILDREN_UNDER_ONE = 1;
    public static final int NUMBER_OF_CHILDREN_UNDER_FOUR = 2;

    public static final BigDecimal VOUCHER_VALUE = new BigDecimal("3.1");
    public static final int VOUCHERS_FOR_CHILDREN_UNDER_ONE = 2;
    public static final int VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR = 1;
    public static final int VOUCHERS_FOR_PREGNANCY = 1;
    public static final int TOTAL_VOUCHER_ENTITLEMENT = 4;
    public static final BigDecimal TOTAL_VOUCHER_VALUE = new BigDecimal("12.4");

}
