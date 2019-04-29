package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Responsible for calculating how many 'vouchers' an eligible claimant is entitled to.
 * The number of vouchers is based on:
 * (2) vouchers per child under one, plus
 * (1) voucher per child between one and four, plus
 * (1) voucher per pregnancy
 * (numbers in brackets are configurable).
 */
@Component
@Slf4j
public class EntitlementCalculator {

    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private final int vouchersPerChildUnderOne;
    private final int vouchersPerChildBetweenOneAndFour;
    private final int vouchersPerPregnancy;
    private final int voucherValueInPence;

    public EntitlementCalculator(
            PregnancyEntitlementCalculator pregnancyEntitlementCalculator,
            @Value("${entitlement.number-of-vouchers-per-child-under-one}") int vouchersPerChildUnderOne,
            @Value("${entitlement.number-of-vouchers-per-child-between-one-and-four}") int vouchersPerChildBetweenOneAndFour,
            @Value("${entitlement.number-of-vouchers-per-pregnancy}") int vouchersPerPregnancy,
            @Value("${entitlement.voucher-value-in-pence}") int voucherValueInPence
    ) {

        this.pregnancyEntitlementCalculator = pregnancyEntitlementCalculator;
        this.vouchersPerChildUnderOne = vouchersPerChildUnderOne;
        this.vouchersPerChildBetweenOneAndFour = vouchersPerChildBetweenOneAndFour;
        this.vouchersPerPregnancy = vouchersPerPregnancy;
        this.voucherValueInPence = voucherValueInPence;
    }

    /**
     * Calculates the {@link VoucherEntitlement} a claimant is entitled to for a given date. The number of children under one and
     * the number between one and four is calculated for the given date.
     *
     * @param claimant              the claimant who will be receiving the vouchers
     * @param dateOfBirthOfChildren the date of birth of the claimant's children
     * @param entitlementDate       the date to check entitlement for
     * @return the voucher entitlement calculated for the claimant
     */
    public VoucherEntitlement calculateVoucherEntitlement(Claimant claimant, List<LocalDate> dateOfBirthOfChildren, LocalDate entitlementDate) {
        int numberOfChildrenUnderFour = getNumberOfChildrenUnderFour(dateOfBirthOfChildren, entitlementDate);
        int numberOfChildrenUnderOne = getNumberOfChildrenUnderOne(dateOfBirthOfChildren, entitlementDate);

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})",
                    numberOfChildrenUnderFour, numberOfChildrenUnderOne);
            throw new IllegalArgumentException("Number of children under four must not be less than number of children under one");
        }

        boolean isEntitledToPregnancyVoucher = pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate(), entitlementDate);

        return createVoucherEntitlement(isEntitledToPregnancyVoucher, numberOfChildrenUnderFour, numberOfChildrenUnderOne);
    }

    private Integer getNumberOfChildrenUnderOne(List<LocalDate> dateOfBirthOfChildren, LocalDate entitlementDate) {
        return getNumberOfChildrenUnderAgeInYears(dateOfBirthOfChildren, entitlementDate, 1);
    }

    private Integer getNumberOfChildrenUnderFour(List<LocalDate> dateOfBirthOfChildren, LocalDate entitlementDate) {
        return getNumberOfChildrenUnderAgeInYears(dateOfBirthOfChildren, entitlementDate, 4);
    }

    private Integer getNumberOfChildrenUnderAgeInYears(List<LocalDate> dateOfBirthOfChildren, LocalDate entitlementDate, Integer ageInYears) {
        if (isEmpty(dateOfBirthOfChildren)) {
            return 0;
        }
        LocalDate pastDate = entitlementDate.minusYears(ageInYears);
        return Math.toIntExact(dateOfBirthOfChildren.stream()
                .filter(date -> date.isAfter(pastDate))
                .count());
    }

    private VoucherEntitlement createVoucherEntitlement(boolean isEntitledToPregnancyVoucher, int numberOfChildrenUnderFour, int numberOfChildrenUnderOne) {
        int numberOfChildrenBetweenOneAndFour = numberOfChildrenUnderFour - numberOfChildrenUnderOne;

        int vouchersForPregnancy = calculateVouchersForPregnancy(isEntitledToPregnancyVoucher);
        int vouchersForChildrenUnderOne = calculateVouchersForChildrenUnderOne(numberOfChildrenUnderOne);
        int vouchersForChildrenBetweenOneAndFour = calculateVouchersForChildrenBetweenOneAndFour(numberOfChildrenBetweenOneAndFour);

        return VoucherEntitlement.builder()
                .vouchersForPregnancy(vouchersForPregnancy)
                .vouchersForChildrenUnderOne(vouchersForChildrenUnderOne)
                .vouchersForChildrenBetweenOneAndFour(vouchersForChildrenBetweenOneAndFour)
                .voucherValueInPence(voucherValueInPence)
                .build();
    }

    private int calculateVouchersForPregnancy(boolean isEntitledToPregnancyVoucher) {
        return isEntitledToPregnancyVoucher ? vouchersPerPregnancy : 0;
    }

    private int calculateVouchersForChildrenUnderOne(int numberOfChildrenUnderOne) {
        return numberOfChildrenUnderOne * vouchersPerChildUnderOne;
    }

    private int calculateVouchersForChildrenBetweenOneAndFour(int numberOfChildrenBetweenOneAndFour) {
        return numberOfChildrenBetweenOneAndFour * vouchersPerChildBetweenOneAndFour;
    }

}
