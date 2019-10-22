package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ReportPropertiesFactory;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.Map;

/**
 * Responsible for reporting 'events' to Google Analytics (events including new claims and payments made, etc).
 * Also responsible for obtaining postcode data used to provide location info when reporting events.
 */
@Component
@Slf4j
@AllArgsConstructor
public class MIReporter {

    private final ClaimRepository claimRepository;
    private final PostcodeDataClient postcodeDataClient;
    private final ReportPropertiesFactory reportPropertiesFactory;
    private final GoogleAnalyticsClient googleAnalyticsClient;

    public void reportClaim(ReportClaimMessageContext context) {
        Claim claim = context.getClaim();
        if (claim.getPostcodeData() == null) {
            PostcodeData postcodeData = postcodeDataClient.getPostcodeData(claim);
            claim.setPostcodeData(postcodeData);
            claimRepository.save(claim);
        }

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        googleAnalyticsClient.reportEvent(reportProperties);
    }

}
