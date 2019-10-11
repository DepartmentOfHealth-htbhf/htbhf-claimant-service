package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_POSTCODE;

@ExtendWith(MockitoExtension.class)
class ReportPropertiesFactoryTest {

    @Mock
    private ClaimantCategoryCalculator claimantCategoryCalculator;

    @InjectMocks
    private ReportPropertiesFactory reportPropertiesFactory;

    @Test
    void shouldCreateReportPropertiesForNewClaim() {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(VALID_POSTCODE);
        Claim claim = aClaimWithPostcodeData(postcodeData);
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(1);
        String claimantCategory = ClaimantCategory.PREGNANT_WITH_CHILDREN.getDescription();
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(6));
        ReportClaimMessageContext context = ReportClaimMessageContext.builder()
                .claimAction(NEW)
                .claim(claim)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .timestamp(timeStamp)
                .build();
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(claimantCategory);

        Map<String, Object> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        // Queue time is number of milliseconds since now and the timestamp value. Therefore a timestamp one second ago should have a queue time >= 1000
        assertThat((Long) reportProperties.get("queueTime")).isGreaterThanOrEqualTo(1000);
        assertThat(reportProperties).contains(
                entry("hitType", "event"),
                entry("eventCategory", "CLAIM"),
                entry("eventAction", "NEW"),
                entry("eventValue", 0),
                entry("cid", claim.getId()),
                entry("dimension1", "ONLINE"), // user type
                entry("dimension3", claimantCategory), // claimant category
                entry("dimension4", postcodeData.getAdminDistrict()), // local authority (admin district in postcodes.io).
                entry("dimension5", postcodeData.getCodes().getAdminDistrict()), // local authority code
                entry("dimension6", postcodeData.getCountry()),
                entry("dimension7", postcodeData.getOutcode()),
                entry("dimension8", postcodeData.getParliamentaryConstituency()),
                entry("dimension9", postcodeData.getCcg()), // Clinical Commissioning Group
                entry("dimension10", postcodeData.getCodes().getCcg())); // Clinical Commissioning Group code
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timeStamp);
    }
}
