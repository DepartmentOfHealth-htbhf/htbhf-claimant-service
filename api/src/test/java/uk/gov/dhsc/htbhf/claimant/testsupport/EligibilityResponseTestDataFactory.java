package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.nCopies;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityResponseTestDataFactory {

    public static EligibilityResponse anEligibilityResponse() {
        return aValidEligibilityResponseBuilder().build();
    }

    public static EligibilityResponse anEligibilityResponseWithChildren(List<ChildDTO> children) {
        return aValidEligibilityResponseBuilder().children(children).build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatus(EligibilityStatus eligibilityStatus) {
        return aValidEligibilityResponseBuilder().eligibilityStatus(eligibilityStatus).build();
    }

    public static EligibilityResponse anEligibilityResponseWithChildrenAndStatus(List<ChildDTO> children, EligibilityStatus eligibilityStatus) {
        return aValidEligibilityResponseBuilder().children(children).eligibilityStatus(eligibilityStatus).build();
    }

    public static EligibilityResponse anEligibilityResponseWithDwpHouseholdIdentifier(String dwpHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().dwpHouseholdIdentifier(dwpHouseholdIdentifier).build();
    }

    public static EligibilityResponse anEligibilityResponseWithHmrcHouseholdIdentifier(String hmrcHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().hmrcHouseholdIdentifier(hmrcHouseholdIdentifier).build();
    }

    public static List<ChildDTO> childrenWithBirthdates(List<LocalDate> datesOfBirth) {
        return datesOfBirth.stream().map(dob -> new ChildDTO(dob)).collect(Collectors.toList());
    }

    private static EligibilityResponse.EligibilityResponseBuilder aValidEligibilityResponseBuilder() {
        List<ChildDTO> children = createChildren(NUMBER_OF_CHILDREN_UNDER_ONE, NUMBER_OF_CHILDREN_UNDER_FOUR);
        return EligibilityResponse.builder()
                .eligibilityStatus(ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .children(children);
    }

    private static List<ChildDTO> createChildren(Integer numberOfChildrenUnderOne, Integer numberOfChildrenUnderFour) {
        List<ChildDTO> childrenUnderOne = nCopies(numberOfChildrenUnderOne, new ChildDTO(MAGGIE_DOB));
        List<ChildDTO> childrenBetweenOneAndFour = nCopies(numberOfChildrenUnderFour - numberOfChildrenUnderOne, new ChildDTO(LISA_DOB));
        return Stream.concat(childrenUnderOne.stream(), childrenBetweenOneAndFour.stream()).collect(Collectors.toList());
    }

}
