package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome;
import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.nCopies;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_FOUR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.MAGGIE_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityNotConfirmedResponse;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityAndEntitlementTestDataFactory {

    public static EligibilityAndEntitlementDecision anEligibleDecision() {
        return aDecisionWithStatus(ELIGIBLE);
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatus(EligibilityStatus eligibilityStatus) {
        EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder = aValidDecisionBuilder().eligibilityStatus(eligibilityStatus);
        removeEligibilityStatusIfAppropriate(eligibilityStatus, builder);
        return builder.build();
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatusAndChildren(EligibilityStatus eligibilityStatus,
                                                                                   EligibilityOutcome eligibilityOutcome,
                                                                                   List<LocalDate> childrenDobs) {
        EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder = aValidDecisionBuilder().eligibilityStatus(eligibilityStatus);
        removeEligibilityStatusIfAppropriate(eligibilityStatus, builder);
        IdentityAndEligibilityResponse identityAndEligibilityResponse = (EligibilityOutcome.CONFIRMED == eligibilityOutcome)
                ? anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs)
                : anIdentityMatchedEligibilityNotConfirmedResponse();
        return builder
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .dateOfBirthOfChildren(childrenDobs)
                .build();
    }

    private static void removeEligibilityStatusIfAppropriate(EligibilityStatus eligibilityStatus,
                                                             EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder) {
        if (eligibilityStatus != ELIGIBLE) {
            builder.voucherEntitlement(null);
        }
    }

    public static EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder aValidDecisionBuilder() {
        List<LocalDate> children = createChildren(NUMBER_OF_CHILDREN_UNDER_ONE, NUMBER_OF_CHILDREN_UNDER_FOUR);
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .identityAndEligibilityResponse(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(children))
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .dateOfBirthOfChildren(children);
    }

    private static List<LocalDate> createChildren(Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour) {
        List<LocalDate> childrenUnderOne = nCopies(numberOfChildrenUnderOne, MAGGIE_DATE_OF_BIRTH);
        List<LocalDate> childrenBetweenOneAndFour = nCopies(numberOfChildrenUnderFour - numberOfChildrenUnderOne, LISA_DOB);
        return newArrayList(concat(childrenUnderOne, childrenBetweenOneAndFour));
    }

}
