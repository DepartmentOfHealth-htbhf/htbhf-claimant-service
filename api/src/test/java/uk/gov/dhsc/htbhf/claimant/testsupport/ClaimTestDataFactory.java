package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.dhsc.htbhf.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.TestConstants.SIMPSONS_POSTCODE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

public class ClaimTestDataFactory {

    public static Claim aValidClaim() {
        return aClaimWithClaimStatus(ClaimStatus.ACTIVE);
    }

    public static Claim aClaimWithTooLongFirstName() {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(ClaimantTestDataFactory.aClaimantWithTooLongFirstName())
                .build();
    }

    public static Claim aClaimWithLastName(String lastName) {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(ClaimantTestDataFactory.aClaimantWithLastName(lastName))
                .build();
    }

    public static Claim aClaimWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate(expectedDeliveryDate))
                .build();
    }

    public static Claim aClaimWithExpectedDeliveryDateAndChildrenDobs(LocalDate expectedDeliveryDate, List<LocalDate> childrenDobs) {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(
                        ClaimantTestDataFactory.aValidClaimantBuilder()
                                .expectedDeliveryDate(expectedDeliveryDate)
                                .childrenDob(childrenDobs)
                                .build())
                .build();
    }

    public static Claim aClaimWithClaimStatus(ClaimStatus claimStatus) {
        return aValidClaimBuilderWithStatus(claimStatus)
                .build();
    }

    public static Claim aClaimWithClaimStatusAndCardStatus(ClaimStatus claimStatus, CardStatus cardStatus) {
        return aValidClaimBuilderWithStatus(claimStatus)
                .cardStatus(cardStatus)
                .build();
    }

    public static Claim aClaimWithClaimStatusAndClaimStatusTimestamp(ClaimStatus claimStatus, LocalDateTime claimStatusTimestamp) {
        return aValidClaimBuilderWithStatus(claimStatus)
                .claimStatusTimestamp(claimStatusTimestamp)
                .build();
    }

    public static Claim aClaimWithEligibilityStatus(EligibilityStatus eligibilityStatus) {
        return aValidClaimBuilder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    public static Claim aClaimWithClaimant(Claimant claimant) {
        return aValidClaimBuilder()
                .claimant(claimant)
                .build();
    }

    public static Claim aClaimWithPostcodeData(PostcodeData postcodeData) {
        return aValidClaimBuilder()
                .postcodeData(postcodeData)
                .build();
    }

    public static Claim aClaimWithDueDateAndPostcodeData(LocalDate expectedDeliveryDate) {
        Claimant claimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        return aValidClaimBuilder()
                .claimant(claimant)
                .postcodeData(aPostcodeDataObjectForPostcode(SIMPSONS_POSTCODE))
                .build();
    }

    public static Claim aClaimWithCardStatusAndCardStatusTimestamp(CardStatus cardStatus, LocalDateTime cardStatusTimestamp) {
        return aValidClaimBuilder()
                .cardStatus(cardStatus)
                .cardStatusTimestamp(cardStatusTimestamp)
                .build();
    }

    public static Claim.ClaimBuilder aValidClaimBuilder() {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE);
    }

    private static Claim.ClaimBuilder aValidClaimBuilderWithStatus(ClaimStatus claimStatus) {
        return Claim.builder()
                .claimant(aValidClaimant())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .claimStatusTimestamp(LocalDateTime.now())
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .claimStatus(claimStatus)
                .cardAccountId(CARD_ACCOUNT_ID)
                .cardStatus(CardStatus.ACTIVE)
                .initialIdentityAndEligibilityResponse(CombinedIdAndEligibilityResponseTestDataFactory
                        .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches())
                .currentIdentityAndEligibilityResponse(CombinedIdAndEligibilityResponseTestDataFactory
                        .anIdMatchedEligibilityConfirmedUCResponseWithAllMatches())
                .cardStatusTimestamp(LocalDateTime.now());
    }

}
