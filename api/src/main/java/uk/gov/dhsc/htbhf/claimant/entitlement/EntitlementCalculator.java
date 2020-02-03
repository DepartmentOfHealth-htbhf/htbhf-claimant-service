package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.dwp.model.QualifyingReason;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator.getNumberOfChildrenUnderFour;
import static uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator.getNumberOfChildrenUnderOne;

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
            @Value("${entitlement.voucher-value-in-pence}") int voucherValueInPence) {
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
     * @param expectedDueDate       the expected due date of the claimant. Use Empty.optional() if the claimant is not pregnant
     * @param dateOfBirthOfChildren the date of birth of the claimant's children
     * @param entitlementDate       the date to check entitlement for
     * @param qualifyingReason  overrides the reason that this applicant qualifies for Healthy Start
     * @return the voucher entitlement calculated for the claimant
     */
    public VoucherEntitlement calculateVoucherEntitlement(Optional<LocalDate> expectedDueDate,
                                                          List<LocalDate> dateOfBirthOfChildren,
                                                          LocalDate entitlementDate,
                                                          QualifyingReason qualifyingReason) {
        int numberOfChildrenUnderFour = getNumberOfChildrenUnderFour(dateOfBirthOfChildren, entitlementDate);
        int numberOfChildrenUnderOne = getNumberOfChildrenUnderOne(dateOfBirthOfChildren, entitlementDate);

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})",
                    numberOfChildrenUnderFour, numberOfChildrenUnderOne);
            throw new IllegalArgumentException("Number of children under four must not be less than number of children under one");
        }

        // call pregnancyEntitlementCalculator if the expectedDueDate exists
        boolean isEntitledToPregnancyVoucher = expectedDueDate
                .map(dueDate -> pregnancyEntitlementCalculator.isEntitledToVoucher(dueDate, entitlementDate, qualifyingReason)).orElse(false);

        return createVoucherEntitlement(isEntitledToPregnancyVoucher, numberOfChildrenUnderFour, numberOfChildrenUnderOne, entitlementDate);
    }

    private VoucherEntitlement createVoucherEntitlement(boolean isEntitledToPregnancyVoucher,
                                                        int numberOfChildrenUnderFour,
                                                        int numberOfChildrenUnderOne,
                                                        LocalDate entitlementDate) {
        int numberOfChildrenBetweenOneAndFour = numberOfChildrenUnderFour - numberOfChildrenUnderOne;

        int vouchersForPregnancy = calculateVouchersForPregnancy(isEntitledToPregnancyVoucher);
        int vouchersForChildrenUnderOne = calculateVouchersForChildrenUnderOne(numberOfChildrenUnderOne);
        int vouchersForChildrenBetweenOneAndFour = calculateVouchersForChildrenBetweenOneAndFour(numberOfChildrenBetweenOneAndFour);

        return VoucherEntitlement.builder()
                .vouchersForPregnancy(vouchersForPregnancy)
                .vouchersForChildrenUnderOne(vouchersForChildrenUnderOne)
                .vouchersForChildrenBetweenOneAndFour(vouchersForChildrenBetweenOneAndFour)
                .singleVoucherValueInPence(voucherValueInPence)
                .entitlementDate(entitlementDate)
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
