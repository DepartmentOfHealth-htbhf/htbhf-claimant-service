package uk.gov.dhsc.htbhf.claimant.testsupport;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.singletonList;

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

    public static final String CARD_ACCOUNT_ID = "bc78da28-4918-45fd-95ca-8bd82979c584";

    public static final int AVAILABLE_BALANCE_IN_PENCE = 100;
    public static final int LEDGER_BALANCE_IN_PENCE = 100;

    public static final String TEST_EXCEPTION_MESSAGE = "test exception";
    public static final RuntimeException TEST_EXCEPTION = new RuntimeException(TEST_EXCEPTION_MESSAGE);
    public static final LocalDate OVERRIDE_UNTIL_FIVE_YEARS = LocalDate.now().plusYears(5);
    public static final LocalDate OVERRIDE_UNTIL_TWENTY_NINE_WEEKS = LocalDate.now().plusWeeks(29);
    public static final List<LocalDate> CHILD_BORN_IN_FUTURE = singletonList(LocalDate.now().plusDays(1));
    public static final String NED_CLAIM_REFERENCE = "0E1567C0B2";
    public static final String HOME_CLAIM_REFERENCE = "0E1567C0B3";
    public static final String MARGE_CLAIM_REFERENCE = "0E1567C0B4";

}
