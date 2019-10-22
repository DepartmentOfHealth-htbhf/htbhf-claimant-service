package uk.gov.dhsc.htbhf.claimant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8120)
public class ReportClaimIntegrationTest {

    private static final String POSTCODES_IO_PATH = "/postcodes/";
    private static final String REPORT_ENDPOINT = "/collect";

    @Value("${google-analytics.tracking-id}")
    private String trackingId;
    @Autowired
    private ClaimMessageSender claimMessageSender;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ClaimRepository claimRepository;

    @Test
    void shouldReportNewClaimToGoogleAnalyticsWithPostcodeData() throws JsonProcessingException {
        Claim claim = aValidClaim();
        claimRepository.save(claim);
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodeDataLookup(postcode);
        stubGoogleAnalyticsCall();

        claimMessageSender.sendReportClaimMessage(claim, ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, ClaimAction.NEW);
        messageProcessorScheduler.processReportClaimMessages();

        verifyPostcodesIoCalled(postcode);
        verifyGoogleAnalyticsCalled(claim);
    }

    private void stubPostcodeDataLookup(String postcode) throws JsonProcessingException {
        String responseBody = objectMapper.writeValueAsString(createPostcodeData(postcode));
        String postcodeWithoutSpace = postcode.replace(" ", "");
        stubFor(get(urlEqualTo(POSTCODES_IO_PATH + postcodeWithoutSpace))
                .willReturn(okJson(responseBody)));
    }

    private void stubGoogleAnalyticsCall() {
        stubFor(post(urlEqualTo(REPORT_ENDPOINT)).withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .willReturn(ok()));
    }

    private PostcodeDataResponse createPostcodeData(String postcode) {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        return new PostcodeDataResponse(postcodeData);
    }

    private void verifyPostcodesIoCalled(String postcode) {
        String expectedPostcodesUrl = getExpectedPostcodeUrl(postcode);
        verify(exactly(1), getRequestedFor(urlEqualTo(expectedPostcodesUrl)));
    }

    private String getExpectedPostcodeUrl(String postcode) {
        String postcodeWithoutSpace = postcode.replace(" ", "");
        return POSTCODES_IO_PATH + postcodeWithoutSpace;
    }

    private void verifyGoogleAnalyticsCalled(Claim claim) {
        // not asserting the full payload as it contains time based values and a large amount of data that would make the test fragile.
        // testing that the payload is created and sent correctly is covered by GoogleAnalyticsClientTest
        verify(exactly(1), postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(matching(
                        "t=event"
                                + "&v=1" // version 1
                                + "&tid=" + trackingId // tracking id from properties
                                + "&ec=CLAIM" // event category is CLAIM
                                + "&ea=NEW" // event action is NEW
                                + "&ev=0" // event value is unused, so set to 0
                                + "&qt=\\d+" // queue time should be an integer
                                + "&cid=" + claim.getId() // customer id is the claim id
                                + ".*"))); // rest of payload data
    }
}
