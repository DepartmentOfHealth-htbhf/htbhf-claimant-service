package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.dwp.model.*;

public class VerificationResultTestDataFactory {

    public static VerificationResult anAllMatchedVerificationResult() {
        return VerificationResult.builder()
                .identityOutcome(IdentityOutcome.MATCHED)
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                .mobilePhoneMatch(VerificationOutcome.MATCHED)
                .emailAddressMatch(VerificationOutcome.MATCHED)
                .addressLine1Match(VerificationOutcome.MATCHED)
                .postcodeMatch(VerificationOutcome.MATCHED)
                .qualifyingBenefits(QualifyingBenefits.UNIVERSAL_CREDIT)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .isPregnantOrAtLeast1ChildMatched(true)
                .build();
    }

    public static VerificationResult anAllMatchedVerificationResultWithPhoneAndEmail(VerificationOutcome phoneMatched, VerificationOutcome emailMatched) {
        return VerificationResult.builder()
                .identityOutcome(IdentityOutcome.MATCHED)
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                .mobilePhoneMatch(phoneMatched)
                .emailAddressMatch(emailMatched)
                .addressLine1Match(VerificationOutcome.MATCHED)
                .postcodeMatch(VerificationOutcome.MATCHED)
                .qualifyingBenefits(QualifyingBenefits.UNIVERSAL_CREDIT)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .isPregnantOrAtLeast1ChildMatched(true)
                .build();
    }

    public static VerificationResult aNothingMatchedVerificationResult() {
        return VerificationResult.builder()
                .identityOutcome(IdentityOutcome.NOT_MATCHED)
                .eligibilityOutcome(EligibilityOutcome.NOT_CONFIRMED)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .isPregnantOrAtLeast1ChildMatched(false)
                .build();
    }

    public static VerificationResult anIdMatchedEligibilityNotConfirmedVerificationResult() {
        return VerificationResult.builder()
                .identityOutcome(IdentityOutcome.MATCHED)
                .eligibilityOutcome(EligibilityOutcome.NOT_CONFIRMED)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .isPregnantOrAtLeast1ChildMatched(true)
                .build();
    }

    public static VerificationResult aPostcodeMismatchVerificationResult() {
        return anEligibleDefaultBuilder()
                .addressLine1Match(VerificationOutcome.MATCHED)
                .postcodeMatch(VerificationOutcome.NOT_MATCHED)
                .build();
    }

    public static VerificationResult anAddressLine1MismatchVerificationResult() {
        return anEligibleDefaultBuilder()
                .addressLine1Match(VerificationOutcome.NOT_MATCHED)
                .postcodeMatch(VerificationOutcome.MATCHED)
                .build();
    }

    public static VerificationResult aFullAddressMismatchVerificationResult() {
        return anEligibleDefaultBuilder()
                .addressLine1Match(VerificationOutcome.NOT_MATCHED)
                .postcodeMatch(VerificationOutcome.NOT_MATCHED)
                .build();
    }

    private static VerificationResult.VerificationResultBuilder anEligibleDefaultBuilder() {
        return VerificationResult.builder()
                .identityOutcome(IdentityOutcome.MATCHED)
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .isPregnantOrAtLeast1ChildMatched(false);
    }
}
