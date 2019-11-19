package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.time.LocalDate;

public class TestConstants {

    public static final int NUMBER_OF_CHILDREN_UNDER_ONE = 1;
    public static final int NUMBER_OF_CHILDREN_UNDER_FOUR = 2;

    public static final int VOUCHER_VALUE_IN_PENCE = 310;
    public static final int VOUCHERS_FOR_CHILDREN_UNDER_ONE = 2;
    public static final int VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR = 1;
    public static final int VOUCHERS_FOR_PREGNANCY = 1;
    public static final int TOTAL_VOUCHER_ENTITLEMENT = 4;
    public static final int TOTAL_VOUCHER_VALUE_IN_PENCE = 1240;

    public static final LocalDate EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS = LocalDate.now().plusMonths(2);
    public static final LocalDate EXPECTED_DELIVERY_DATE_TOO_FAR_IN_PAST = LocalDate.now().minusWeeks(13);
    public static final LocalDate NOT_PREGNANT = null;

    public static final String CARD_ACCOUNT_ID = "123456789";

    public static final int AVAILABLE_BALANCE_IN_PENCE = 100;
    public static final int LEDGER_BALANCE_IN_PENCE = 100;

    public static final String TEST_EXCEPTION_MESSAGE = "test exception";
    public static final RuntimeException TEST_EXCEPTION = new RuntimeException(TEST_EXCEPTION_MESSAGE);

}
