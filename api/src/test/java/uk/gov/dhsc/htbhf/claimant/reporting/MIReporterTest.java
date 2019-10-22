package uk.gov.dhsc.htbhf.claimant.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ReportPropertiesFactory;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.HIT_TYPE_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.PROTOCOL_VERSION_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.TRACKING_ID_KEY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_POSTCODE;

@ExtendWith(MockitoExtension.class)
class MIReporterTest {

    private static final Map<String, String> REPORT_PROPERTIES = Map.of(
            HIT_TYPE_KEY.getFieldName(), "event",
            PROTOCOL_VERSION_KEY.getFieldName(), "1",
            TRACKING_ID_KEY.getFieldName(), "tracking-id");

    @Mock
    private PostcodeDataClient postcodeDataClient;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ReportPropertiesFactory reportPropertiesFactory;
    @Mock
    private GoogleAnalyticsClient googleAnalyticsClient;

    @InjectMocks
    private MIReporter miReporter;

    @Test
    void shouldGetPostcodeDataAndSaveToClaimAndCallGoogleAnalytics() {
        Claim claim = aClaimWithPostcodeData(null);
        ReportClaimMessageContext context = ReportClaimMessageContext.builder().claim(claim).build();
        String postcode = claim.getClaimant().getAddress().getPostcode();
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        given(postcodeDataClient.getPostcodeData(any())).willReturn(postcodeData);
        given(reportPropertiesFactory.createReportPropertiesForClaimEvent(any())).willReturn(REPORT_PROPERTIES);

        miReporter.reportClaim(context);

        assertThat(claim.getPostcodeData()).isEqualTo(postcodeData);
        verify(postcodeDataClient).getPostcodeData(claim);
        verify(claimRepository).save(claim);
        verify(reportPropertiesFactory).createReportPropertiesForClaimEvent(context);
        verify(googleAnalyticsClient).reportEvent(REPORT_PROPERTIES);
    }

    @Test
    void shouldNotGetPostcodeDataOrUpdateClaimWhenPostcodeDataExists() {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(VALID_POSTCODE);
        Claim claim = aClaimWithPostcodeData(postcodeData);
        ReportClaimMessageContext context = ReportClaimMessageContext.builder().claim(claim).build();
        given(reportPropertiesFactory.createReportPropertiesForClaimEvent(any())).willReturn(REPORT_PROPERTIES);

        miReporter.reportClaim(context);

        verifyZeroInteractions(postcodeDataClient, claimRepository);
        verify(reportPropertiesFactory).createReportPropertiesForClaimEvent(context);
        verify(googleAnalyticsClient).reportEvent(REPORT_PROPERTIES);
    }
}
