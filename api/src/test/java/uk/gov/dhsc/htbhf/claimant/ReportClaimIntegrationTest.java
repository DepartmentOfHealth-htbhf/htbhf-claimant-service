package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.REJECTED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataResponseTestFactory.aPostcodeDataResponseObjectForPostcode;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ReportClaimIntegrationTest {

    @Value("${google-analytics.tracking-id}")
    private String trackingId;
    @Autowired
    private ClaimMessageSender claimMessageSender;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private WiremockManager wiremockManager;

    @BeforeEach
    void setup() {
        wiremockManager.startWireMock();
    }

    @AfterEach
    void tearDown() {
        wiremockManager.stopWireMock();
    }

    @Test
    void shouldReportNewClaimToGoogleAnalyticsWithPostcodeData() throws JsonProcessingException {
        Claim claim = aValidClaim();
        claimRepository.save(claim);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodesIoAndGoogleAnalytics(postcode);

        claimMessageSender.sendReportClaimMessage(claim, ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, ClaimAction.NEW);
        messageProcessorScheduler.processReportClaimMessages();

        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForClaimEvent(claim, ClaimAction.NEW, trackingId);
    }

    @Test
    void shouldReportRejectedClaimToGoogleAnalyticsWithPostcodeData() throws JsonProcessingException {
        Claim claim = aClaimWithClaimStatus(REJECTED);
        claimRepository.save(claim);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodesIoAndGoogleAnalytics(postcode);

        claimMessageSender.sendReportClaimMessage(claim, ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, ClaimAction.REJECTED);
        messageProcessorScheduler.processReportClaimMessages();

        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForClaimEvent(claim, ClaimAction.REJECTED, trackingId);
    }

    private void stubPostcodesIoAndGoogleAnalytics(String postcode) throws JsonProcessingException {
        PostcodeDataResponse postcodeDataResponse = aPostcodeDataResponseObjectForPostcode(postcode);
        wiremockManager.stubPostcodeDataLookup(postcodeDataResponse);
        wiremockManager.stubGoogleAnalyticsCall();
    }
}
