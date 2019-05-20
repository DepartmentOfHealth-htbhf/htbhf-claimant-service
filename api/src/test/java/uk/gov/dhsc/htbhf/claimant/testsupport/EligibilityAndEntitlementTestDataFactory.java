package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.nCopies;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityAndEntitlementTestDataFactory {

    public static EligibilityAndEntitlementDecision anEligibilityAndEntitlementDecisionWithStatus(EligibilityStatus eligibilityStatus) {
        return aValidEligibilityAndEntitlementBuilder().eligibilityStatus(eligibilityStatus).build();
    }

    public static EligibilityAndEntitlementDecision anEligibilityAndEntitlementDecisionWithStatusAndEntitlement(EligibilityStatus eligibilityStatus,
                                                                                                                PaymentCycleVoucherEntitlement entitlement) {
        return aValidEligibilityAndEntitlementBuilder()
                .eligibilityStatus(eligibilityStatus)
                .voucherEntitlement(entitlement)
                .build();
    }

    private static EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder aValidEligibilityAndEntitlementBuilder() {
        List<LocalDate> children = createChildren(NUMBER_OF_CHILDREN_UNDER_ONE, NUMBER_OF_CHILDREN_UNDER_FOUR);
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .dateOfBirthOfChildren(children);
    }

    private static List<LocalDate> createChildren(Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour) {
        List<LocalDate> childrenUnderOne = nCopies(numberOfChildrenUnderOne, MAGGIE_DOB);
        List<LocalDate> childrenBetweenOneAndFour = nCopies(numberOfChildrenUnderFour - numberOfChildrenUnderOne, LISA_DOB);
        return newArrayList(concat(childrenUnderOne, childrenBetweenOneAndFour));
    }

}
