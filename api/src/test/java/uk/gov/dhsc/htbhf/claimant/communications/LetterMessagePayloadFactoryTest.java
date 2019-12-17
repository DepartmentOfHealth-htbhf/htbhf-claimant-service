package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.message.LetterTemplateKey.*;

@ExtendWith(MockitoExtension.class)
class LetterMessagePayloadFactoryTest {

    @Test
    void shouldCreatePayloadWithCorrectAddress() {
        Claim claim = ClaimTestDataFactory.aValidClaim();

        LetterMessagePayload payload = LetterMessagePayloadFactory.buildLetterPayloadWithAddressOnly(claim, LetterType.UPDATE_YOUR_ADDRESS);

        assertThat(payload).isNotNull();
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getLetterType()).isEqualTo(LetterType.UPDATE_YOUR_ADDRESS);
        assertThat(payload.getPersonalisation()).containsOnly(
                entry(ADDRESS_LINE_1.getTemplateKeyName(), HOMER_FORENAME + " " + SIMPSON_SURNAME),
                entry(ADDRESS_LINE_2.getTemplateKeyName(), SIMPSONS_ADDRESS_LINE_1),
                entry(ADDRESS_LINE_3.getTemplateKeyName(), SIMPSONS_ADDRESS_LINE_2),
                entry(ADDRESS_LINE_4.getTemplateKeyName(), SIMPSONS_TOWN),
                entry(ADDRESS_LINE_5.getTemplateKeyName(), SIMPSONS_COUNTY),
                entry(POSTCODE.getTemplateKeyName(), SIMPSONS_POSTCODE)
        );
    }
}