package uk.gov.dhsc.htbhf.claimant.factory;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.converter.AddressToAddressDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class CardRequestFactoryTest {

    @MockBean
    private AddressToAddressDTOConverter addressConverter;

    @Autowired
    private CardRequestFactory cardRequestFactory;

    @Test
    void shouldCreateCardRequest() {
        given(addressConverter.convert(any())).willReturn(aValidAddressDTO());
        Claim claim = aValidClaim();

        CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);

        assertThat(cardRequest.getFirstName()).isEqualTo(claim.getClaimant().getFirstName());
        assertThat(cardRequest.getLastName()).isEqualTo(claim.getClaimant().getLastName());
        assertThat(cardRequest.getClaimId()).isEqualTo(claim.getId().toString());
        assertThat(cardRequest.getDateOfBirth()).isEqualTo(claim.getClaimant().getDateOfBirth());
        assertThat(cardRequest.getAddress()).isEqualTo(aValidAddressDTO());
        verify(addressConverter).convert(claim.getClaimant().getCardDeliveryAddress());
    }

    @Test
    void shouldCreateCardRequestWhenGivenNullClaim() {
        assertThatIllegalArgumentException().isThrownBy(() -> cardRequestFactory.createCardRequest(null));
    }
}
