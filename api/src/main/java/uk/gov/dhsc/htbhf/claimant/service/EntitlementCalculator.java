package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

@Component
@AllArgsConstructor
@Slf4j
public class EntitlementCalculator {

    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private static final int vouchersForChildrenUnderOne = 2;
    private static final int vouchersForChildrenBetweenOneAndFour = 1;
    private static final int vouchersForPregnancy = 1;

    public int calculateVoucherEntitlement(Claimant claimant, EligibilityResponse eligibilityResponse) {

        int numberOfChildrenUnderFour = eligibilityResponse.getNumberOfChildrenUnderFour() == null ? 0 : eligibilityResponse.getNumberOfChildrenUnderFour();
        int numberOfChildrenUnderOne = eligibilityResponse.getNumberOfChildrenUnderOne() == null ? 0 : eligibilityResponse.getNumberOfChildrenUnderOne();

        if (numberOfChildrenUnderFour < numberOfChildrenUnderOne) {
            log.error("Number of children under four ({}) must not be less than number of children under one ({})", numberOfChildrenUnderFour, numberOfChildrenUnderOne);
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
