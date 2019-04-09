package uk.gov.dhsc.htbhf.claimant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

@Component
@Slf4j
public class EntitlementCalculator {

    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private final int vouchersForChildrenUnderOne;
    private final int vouchersForChildrenBetweenOneAndFour;
    private final int vouchersForPregnancy;

    public EntitlementCalculator(
            PregnancyEntitlementCalculator pregnancyEntitlementCalculator,
            @Value("${entitlement.vouchers-for-children-under-one}") int vouchersForChildrenUnderOne,
            @Value("${entitlement.vouchers-for-children-between-one-and-four}") int vouchersForChildrenBetweenOneAndFour,
            @Value("${entitlement.vouchers-for-pregnancy}") int vouchersForPregnancy) {

        this.pregnancyEntitlementCalculator = pregnancyEntitlementCalculator;
        this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
        this.vouchersForPregnancy = vouchersForPregnancy;
    }

    public int calculateVoucherEntitlement(Claimant claimant, EligibilityResponse eligibilityResponse) {

        int numberOfChildrenUnderFour = eligibilityResponse.getNumberOfChildrenUnderFour() == null ? 0 : eligibilityResponse.getNumberOfChildrenUnderFour();
        int numberOfChildrenUnderOne = eligibilityResponse.getNumberOfChildrenUnderOne() == null ? 0 : eligibilityResponse.getNumberOfChildrenUnderOne();

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})",
                    numberOfChildrenUnderFour, numberOfChildrenUnderOne);
            throw new IllegalArgumentException("Number of children under four must not be less than number of children under one");
        }

        int numberOfChildrenBetweenOneAndFour = numberOfChildrenUnderFour - numberOfChildrenUnderOne;
        boolean isPregnant = pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate());
        int pregnancyVouchers = isPregnant ? vouchersForPregnancy : 0;
        int vouchersUnderOne = numberOfChildrenUnderOne * vouchersForChildrenUnderOne;
        int vouchersUnderFour = numberOfChildrenBetweenOneAndFour * vouchersForChildrenBetweenOneAndFour;

        return pregnancyVouchers + vouchersUnderOne + vouchersUnderFour;
    }
}
