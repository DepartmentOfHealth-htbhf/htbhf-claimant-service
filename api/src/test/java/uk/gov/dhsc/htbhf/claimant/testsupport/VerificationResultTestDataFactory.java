package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.dwp.model.v2.DeathVerificationFlag;
import uk.gov.dhsc.htbhf.dwp.model.v2.QualifyingBenefits;
import uk.gov.dhsc.htbhf.dwp.model.v2.VerificationOutcome;

public class VerificationResultTestDataFactory {

    public static VerificationResult anAllMatchedVerificationResult() {
        return VerificationResult.builder()
                .mobilePhoneMatch(VerificationOutcome.MATCHED)
                .emailAddressMatch(VerificationOutcome.MATCHED)
                .addressLine1Match(VerificationOutcome.MATCHED)
                .postcodeMatch(VerificationOutcome.MATCHED)
                .qualifyingBenefits(QualifyingBenefits.UNIVERSAL_CREDIT)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .build();
    }

    public static VerificationResult aNothingMatchedVerificationResult() {
        return VerificationResult.builder()
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .build();
    }
}
