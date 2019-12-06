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
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.scheduler.MessageProcessorScheduler;
import uk.gov.dhsc.htbhf.claimant.testsupport.WiremockManager;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.INITIAL_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.TOP_UP_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataResponseTestFactory.aPostcodeDataResponseObjectForPostcode;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ReportPaymentIntegrationTest {

    @Value("${google-analytics.tracking-id}")
    private String trackingId;
    @Autowired
    private ReportPaymentMessageSender reportPaymentMessageSender;
    @Autowired
    private MessageProcessorScheduler messageProcessorScheduler;
    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private PaymentCycleRepository paymentCycleRepository;
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
    void shouldReportInitialPaymentToGoogleAnalytics() throws JsonProcessingException {
        Claim claim = claimRepository.save(aValidClaim());
        PaymentCycle paymentCycle = paymentCycleRepository.save(aPaymentCycleWithClaim(claim));
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodesIoAndGoogleAnalytics(postcode);

        reportPaymentMessageSender.sendReportInitialPaymentMessage(claim, paymentCycle);
        messageProcessorScheduler.processReportPaymentMessages();

        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForPaymentEvent(claim, INITIAL_PAYMENT, trackingId,
                paymentCycle.getTotalEntitlementAmountInPence(), paymentCycle.getChildrenDob());
    }

    @Test
    void shouldReportScheduledPaymentToGoogleAnalytics() throws JsonProcessingException {
        Claim claim = claimRepository.save(aValidClaim());
        PaymentCycle paymentCycle = paymentCycleRepository.save(aPaymentCycleWithClaim(claim));
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodesIoAndGoogleAnalytics(postcode);

        reportPaymentMessageSender.sendReportScheduledPayment(claim, paymentCycle);
        messageProcessorScheduler.processReportPaymentMessages();

        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForPaymentEvent(claim, SCHEDULED_PAYMENT, trackingId,
                paymentCycle.getTotalEntitlementAmountInPence(), paymentCycle.getChildrenDob());
    }

    @Test
    void shouldReportTopUpPaymentToGoogleAnalytics() throws JsonProcessingException {
        Claim claim = claimRepository.save(aValidClaim());
        PaymentCycle paymentCycle = paymentCycleRepository.save(aPaymentCycleWithClaim(claim));
        String postcode = claim.getClaimant().getAddress().getPostcode();
        stubPostcodesIoAndGoogleAnalytics(postcode);
        int paymentAmount = 100;

        reportPaymentMessageSender.sendReportPregnancyTopUpPaymentMessage(claim, paymentCycle, paymentAmount);
        messageProcessorScheduler.processReportPaymentMessages();

        wiremockManager.verifyPostcodesIoCalled(postcode);
        wiremockManager.verifyGoogleAnalyticsCalledForPaymentEvent(claim, TOP_UP_PAYMENT, trackingId, paymentAmount, paymentCycle.getChildrenDob());
    }

    private void stubPostcodesIoAndGoogleAnalytics(String postcode) throws JsonProcessingException {
        PostcodeDataResponse postcodeDataResponse = aPostcodeDataResponseObjectForPostcode(postcode);
        wiremockManager.stubPostcodeDataLookup(postcodeDataResponse);
        wiremockManager.stubGoogleAnalyticsCall();
    }
}
