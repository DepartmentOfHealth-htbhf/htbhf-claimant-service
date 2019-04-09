package uk.gov.dhsc.htbhf.claimant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

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

    public EntitlementCalculator(
            PregnancyEntitlementCalculator pregnancyEntitlementCalculator,
            @Value("${entitlement.number-of-vouchers-per-child-under-one}") int vouchersPerChildUnderOne,
            @Value("${entitlement.number-of-vouchers-per-child-between-one-and-four}") int vouchersPerChildBetweenOneAndFour,
            @Value("${entitlement.number-of-vouchers-per-pregnancy}") int vouchersPerPregnancy) {

        this.pregnancyEntitlementCalculator = pregnancyEntitlementCalculator;
        this.vouchersPerChildUnderOne = vouchersPerChildUnderOne;
        this.vouchersPerChildBetweenOneAndFour = vouchersPerChildBetweenOneAndFour;
        this.vouchersPerPregnancy = vouchersPerPregnancy;
    }

    public int calculateVoucherEntitlement(Claimant claimant, EligibilityResponse eligibilityResponse) {

        int numberOfChildrenUnderFour = zeroIfNull(eligibilityResponse.getNumberOfChildrenUnderFour());
        int numberOfChildrenUnderOne = zeroIfNull(eligibilityResponse.getNumberOfChildrenUnderOne());

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})",
                    numberOfChildrenUnderFour, numberOfChildrenUnderOne);
            throw new IllegalArgumentException("Number of children under four must not be less than number of children under one");
        }

        int numberOfChildrenBetweenOneAndFour = numberOfChildrenUnderFour - numberOfChildrenUnderOne;

        return calculateVouchersForPregnancy(claimant)
                + calculateVouchersForChildrenUnderOne(numberOfChildrenUnderOne)
                + calculateVouchersForChildrenBetweenOneAndFour(numberOfChildrenBetweenOneAndFour);
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

    private int zeroIfNull(Integer count) {
        return count == null ? 0 : count;
    }
}
