package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ReportClaimPropertiesFactory;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ReportPaymentPropertiesFactory;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.List;
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
    private final ReportClaimPropertiesFactory reportClaimPropertiesFactory;
    private final ReportPaymentPropertiesFactory reportPaymentPropertiesFactory;
    private final GoogleAnalyticsClient googleAnalyticsClient;

    public void reportClaim(ReportClaimMessageContext context) {
        Claim claim = context.getClaim();
        if (postcodeDataIsNotSet(claim) || addressHasBeenUpdated(context)) {
            retrievePostcodeDataAndSaveToClaim(claim);
        }
        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);
        googleAnalyticsClient.reportEvent(reportProperties);
    }

    public void reportPayment(ReportPaymentMessageContext context) {
        Claim claim = context.getClaim();
        if (postcodeDataIsNotSet(claim)) {
            retrievePostcodeDataAndSaveToClaim(claim);
        }
        Map<String, String> reportProperties = reportPaymentPropertiesFactory.createReportPropertiesForPaymentEvent(context);
        googleAnalyticsClient.reportEvent(reportProperties);
    }

    private boolean postcodeDataIsNotSet(Claim claim) {
        return claim.getPostcodeData() == null;
    }

    private boolean addressHasBeenUpdated(ReportClaimMessageContext context) {
        List<UpdatableClaimantField> updatedClaimFields = context.getUpdatedClaimFields();
        return updatedClaimFields != null && updatedClaimFields.contains(UpdatableClaimantField.ADDRESS);
    }

    private void retrievePostcodeDataAndSaveToClaim(Claim claim) {
        PostcodeData postcodeData = postcodeDataClient.getPostcodeData(claim);
        claim.setPostcodeData(postcodeData);
        claimRepository.save(claim);
    }
}
