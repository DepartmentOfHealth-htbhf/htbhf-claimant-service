package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.nCopies;
import static uk.gov.dhsc.htbhf.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.LISA_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_FOUR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityAndEntitlementTestDataFactory {

    public static EligibilityAndEntitlementDecision anEligibleDecision() {
        return aDecisionWithStatus(ELIGIBLE);
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatus(EligibilityStatus eligibilityStatus) {
        return aDecisionWithStatusAndExistingClaim(eligibilityStatus, null);
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatusAndExistingClaim(EligibilityStatus eligibilityStatus, UUID existingClaimId) {
        EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder = aValidDecisionBuilder()
                .eligibilityStatus(eligibilityStatus)
                .existingClaimId(existingClaimId);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = (ELIGIBLE == eligibilityStatus)
                ? CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches()
                : CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityNotConfirmedResponse();
        removeVoucherEntitlementIfAppropriate(eligibilityStatus, builder);
        builder.identityAndEligibilityResponse(identityAndEligibilityResponse);
        return builder.build();
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatusAndResponse(EligibilityStatus eligibilityStatus,
                                                                                   CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return aValidDecisionBuilder()
                .eligibilityStatus(eligibilityStatus)
                .identityAndEligibilityResponse(identityAndEligibilityResponse).build();
    }

    public static EligibilityAndEntitlementDecision aDecisionWithStatusAndChildren(EligibilityStatus eligibilityStatus,
                                                                                   EligibilityOutcome eligibilityOutcome,
                                                                                   List<LocalDate> childrenDobs) {
        EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder = aValidDecisionBuilder().eligibilityStatus(eligibilityStatus);
        removeVoucherEntitlementIfAppropriate(eligibilityStatus, builder);
        CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = (EligibilityOutcome.CONFIRMED == eligibilityOutcome)
                ? CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs)
                : CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityNotConfirmedResponse();
        return builder
                .identityAndEligibilityResponse(identityAndEligibilityResponse)
                .build();
    }

    private static void removeVoucherEntitlementIfAppropriate(EligibilityStatus eligibilityStatus,
                                                              EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder builder) {
        if (eligibilityStatus != ELIGIBLE) {
            builder.voucherEntitlement(null);
        }
    }

    public static EligibilityAndEntitlementDecision.EligibilityAndEntitlementDecisionBuilder aValidDecisionBuilder() {
        List<LocalDate> children = createChildren(NUMBER_OF_CHILDREN_UNDER_ONE, NUMBER_OF_CHILDREN_UNDER_FOUR);
        return EligibilityAndEntitlementDecision.builder()
                .eligibilityStatus(ELIGIBLE)
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(children))
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers());
    }

    private static List<LocalDate> createChildren(Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour) {
        List<LocalDate> childrenUnderOne = nCopies(numberOfChildrenUnderOne, MAGGIE_DATE_OF_BIRTH);
        List<LocalDate> childrenBetweenOneAndFour = nCopies(numberOfChildrenUnderFour - numberOfChildrenUnderOne, LISA_DATE_OF_BIRTH);
        return newArrayList(concat(childrenUnderOne, childrenBetweenOneAndFour));
    }

}
