package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.dwp.model.*;

/**
 * Encapsulation of the extra verification outcome information that was returned from DWP.
 */
@Value
@Builder
@ApiModel(description = "The result of verifying the claimant's details")
public class VerificationResult {

    private final IdentityOutcome identityOutcome;

    private final EligibilityOutcome eligibilityOutcome;

    private final DeathVerificationFlag deathVerificationFlag;

    private final VerificationOutcome mobilePhoneMatch;

    private final VerificationOutcome emailAddressMatch;

    private final VerificationOutcome addressLine1Match;

    private final VerificationOutcome postcodeMatch;

    private final VerificationOutcome pregnantChildDOBMatch;

    private final QualifyingBenefits qualifyingBenefits;

    private final Boolean isPregnantOrAtLeast1ChildMatched;

    @JsonIgnore
    public boolean isAddressMismatch() {
        return VerificationOutcome.NOT_MATCHED == addressLine1Match || VerificationOutcome.NOT_MATCHED == postcodeMatch;
    }

}
