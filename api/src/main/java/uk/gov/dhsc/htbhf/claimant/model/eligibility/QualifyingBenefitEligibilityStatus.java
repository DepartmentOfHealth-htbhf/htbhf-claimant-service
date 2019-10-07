package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

/**
 * Enumeration of the possible eligibility statuses provided by DWP and HMRC.
 * See also https://helptobuyhealthyfood.atlassian.net/wiki/spaces/DWPCOL/pages/182910978/Swagger.
 */
public enum QualifyingBenefitEligibilityStatus {
    CONFIRMED,
    NOT_CONFIRMED,
    NOT_SET;

    /**
     * TODO: HTBHF-2388 This is a temporary method until the EligibilityService API is updated to include a more comprehensive response.
     * @param source the {@link EligibilityStatus} to determine this status from.
     * @return the status value appropriate to the source status.
     */
    public static QualifyingBenefitEligibilityStatus fromEligibilityStatus(EligibilityStatus source) {
        if (source == EligibilityStatus.ELIGIBLE) {
            return CONFIRMED;
        } else if (source == EligibilityStatus.INELIGIBLE) {
            return NOT_CONFIRMED;
        }
        return NOT_SET;
    }
}

