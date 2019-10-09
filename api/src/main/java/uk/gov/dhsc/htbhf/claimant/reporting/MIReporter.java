package uk.gov.dhsc.htbhf.claimant.reporting;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

@Component
public class MIReporter {

    public void reportClaim(Claim claim) {
        // TODO DW HTBHF-2418 Populate the claim with post code data and report claim to google analytics
    }
}
