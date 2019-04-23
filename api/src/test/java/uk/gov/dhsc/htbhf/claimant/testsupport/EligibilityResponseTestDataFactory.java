package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.nCopies;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LISA_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MAGGIE_DOB;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_FOUR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NUMBER_OF_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityResponseTestDataFactory {

    public static EligibilityResponse anEligibilityResponse() {
        return aValidEligibilityResponseBuilder().build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatus(EligibilityStatus eligibilityStatus) {
        return aValidEligibilityResponseBuilder().eligibilityStatus(eligibilityStatus).build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatusOnly(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    public static EligibilityResponse anEligibilityResponseWithDwpHouseholdIdentifier(String dwpHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().dwpHouseholdIdentifier(dwpHouseholdIdentifier).build();
    }

    public static EligibilityResponse anEligibilityResponseWithHmrcHouseholdIdentifier(String hmrcHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().hmrcHouseholdIdentifier(hmrcHouseholdIdentifier).build();
    }

    public static EligibilityResponse.EligibilityResponseBuilder aValidEligibilityResponseBuilder() {
        List<ChildDTO> children = createChildren(NUMBER_OF_CHILDREN_UNDER_ONE, NUMBER_OF_CHILDREN_UNDER_FOUR);
        return EligibilityResponse.builder()
                .eligibilityStatus(ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .children(children);
    }

    public static List<ChildDTO> createChildren(Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour) {
        List<ChildDTO> childrenUnderOne = nCopies(numberOfChildrenUnderOne, new ChildDTO(MAGGIE_DOB));
        List<ChildDTO> childrenBetweenOneAndFour = nCopies(numberOfChildrenUnderFour - numberOfChildrenUnderOne, new ChildDTO(LISA_DOB));
        return Stream.concat(childrenUnderOne.stream(), childrenBetweenOneAndFour.stream()).collect(Collectors.toList());
    }

}
