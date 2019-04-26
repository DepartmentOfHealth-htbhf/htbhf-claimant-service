package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsible for calculating how many 'vouchers' an eligible claimant is entitled to.
 * The number of vouchers is based on:
 *  (2) vouchers per child under one, plus
 *  (1) voucher per child between one and four, plus
 *  (1) voucher per pregnancy
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

    public VoucherEntitlement calculateVoucherEntitlement(Claimant claimant, List<LocalDate> childrenDatesOfBirth) {
        int numberOfChildrenUnderFour = getNumberOfChildrenUnderFour(childrenDatesOfBirth);
        int numberOfChildrenUnderOne = getNumberOfChildrenUnderOne(childrenDatesOfBirth);

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})",
                    numberOfChildrenUnderFour, numberOfChildrenUnderOne);
            throw new IllegalArgumentException("Number of children under four must not be less than number of children under one");
        }

        return createVoucherEntitlement(claimant, numberOfChildrenUnderFour, numberOfChildrenUnderOne);
    }

    private Integer getNumberOfChildrenUnderOne(List<LocalDate> dateOfBirths) {
        return getNumberOfChildrenUnderAgeInYears(dateOfBirths, 1);
    }

    private Integer getNumberOfChildrenUnderFour(List<LocalDate> dateOfBirths) {
        return getNumberOfChildrenUnderAgeInYears(dateOfBirths, 4);
    }

    private Integer getNumberOfChildrenUnderAgeInYears(List<LocalDate> dateOfBirths, Integer ageInYears) {
        if (dateOfBirths == null) {
            return 0;
        }
        LocalDate pastDate = LocalDate.now().minusYears(ageInYears);
        return Math.toIntExact(dateOfBirths.stream()
                .filter(date -> date.isAfter(pastDate))
                .count());
    }

    private VoucherEntitlement createVoucherEntitlement(Claimant claimant, int numberOfChildrenUnderFour, int numberOfChildrenUnderOne) {
        int numberOfChildrenBetweenOneAndFour = numberOfChildrenUnderFour - numberOfChildrenUnderOne;

        int vouchersForPregnancy = calculateVouchersForPregnancy(claimant);
        int vouchersForChildrenUnderOne = calculateVouchersForChildrenUnderOne(numberOfChildrenUnderOne);
        int vouchersForChildrenBetweenOneAndFour = calculateVouchersForChildrenBetweenOneAndFour(numberOfChildrenBetweenOneAndFour);

        return VoucherEntitlement.builder()
                .vouchersForPregnancy(vouchersForPregnancy)
                .vouchersForChildrenUnderOne(vouchersForChildrenUnderOne)
                .vouchersForChildrenBetweenOneAndFour(vouchersForChildrenBetweenOneAndFour)
                .voucherValueInPence(voucherValueInPence)
                .build();
    }

    private int calculateVouchersForPregnancy(Claimant claimant) {
        boolean isPregnant = pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate());
        return isPregnant ? vouchersPerPregnancy : 0;
    }

    private int calculateVouchersForChildrenUnderOne(int numberOfChildrenUnderOne) {
        return numberOfChildrenUnderOne * vouchersPerChildUnderOne;
    }

    private int calculateVouchersForChildrenBetweenOneAndFour(int numberOfChildrenBetweenOneAndFour) {
        return numberOfChildrenBetweenOneAndFour * vouchersPerChildBetweenOneAndFour;
    }

}
