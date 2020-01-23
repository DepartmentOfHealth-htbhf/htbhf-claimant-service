package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.dhsc.htbhf.TestConstants.SINGLE_THREE_YEAR_OLD;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.NEW;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithChildrenDobAndDueDateAndPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithDueDateAndPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NOT_PREGNANT;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

public class ReportClaimMessageContextTestDataFactory {

    public static ReportClaimMessageContext aReportClaimMessageContextWithClaimAndUpdatedFields(Claim claim, List<UpdatableClaimantField> updatedFields) {
        return aReportClaimMessageContextBuilder()
                .updatedClaimantFields(updatedFields)
                .claim(claim)
                .build();
    }

    public static ReportClaimMessageContext aReportClaimMessageContextForAnUpdatedClaim(LocalDateTime timestamp,
                                                                                  List<LocalDate> datesOfBirthOfChildren,
                                                                                  LocalDate expectedDeliveryDate,
                                                                                  List<UpdatableClaimantField> updatedClaimantFields) {
        return aReportClaimMessageContextBuilder(timestamp, datesOfBirthOfChildren, expectedDeliveryDate)
                .updatedClaimantFields(updatedClaimantFields)
                .claimAction(UPDATED)
                .build();
    }

    public static ReportClaimMessageContext aReportClaimMessageContext(LocalDateTime timestamp,
                                                                 List<LocalDate> datesOfBirthOfChildren,
                                                                 LocalDate expectedDeliveryDate) {
        return aReportClaimMessageContextBuilder(timestamp, datesOfBirthOfChildren, expectedDeliveryDate).build();
    }

    public static ReportClaimMessageContext aReportClaimMessageContextWithoutDecision(LocalDateTime timestamp, List<LocalDate> datesOfBirthOfChildren) {
        Claim claim = aClaimWithChildrenDobAndDueDateAndPostcodeData(NOT_PREGNANT, datesOfBirthOfChildren);
        return ReportClaimMessageContext.builder()
                .claimAction(NEW)
                .claim(claim)
                .identityAndEligibilityResponse(null)
                .timestamp(timestamp)
                .build();
    }

    private static ReportClaimMessageContext.ReportClaimMessageContextBuilder aReportClaimMessageContextBuilder(LocalDateTime timestamp,
                                                                                                                List<LocalDate> datesOfBirthOfChildren,
                                                                                                                LocalDate expectedDeliveryDate) {
        Claim claim = aClaimWithDueDateAndPostcodeData(expectedDeliveryDate);
        return ReportClaimMessageContext.builder()
                .claimAction(NEW)
                .claim(claim)
                .identityAndEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(datesOfBirthOfChildren))
                .timestamp(timestamp);
    }

    // Without <?, ?> at the end of ReportClaimMessageContextBuilder, an instance of ReportEventMessageContextBuilder is returned instead
    // of a ReportClaimMessageContextBuilder, this is due to how lombok implements it's SuperBuilder.
    private static ReportClaimMessageContext.ReportClaimMessageContextBuilder<?, ?> aReportClaimMessageContextBuilder() {
        return ReportClaimMessageContext.builder()
                .claim(aValidClaim())
                .updatedClaimantFields(List.of(FIRST_NAME, LAST_NAME))
                .timestamp(LocalDateTime.now())
                .identityAndEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(SINGLE_THREE_YEAR_OLD))
                .claimAction(NEW);
    }
}
