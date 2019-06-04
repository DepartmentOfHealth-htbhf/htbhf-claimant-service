package uk.gov.dhsc.htbhf.claimant.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class CardRequestFactoryTest {

    @Mock
    private AddressToAddressDTOConverter addressConverter;

    @InjectMocks
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
        verify(addressConverter).convert(claim.getClaimant().getAddress());
    }

    @Test
    void shouldCreateCardRequestWhenGivenNullClaim() {
        assertThatIllegalArgumentException().isThrownBy(() -> cardRequestFactory.createCardRequest(null));
    }
}
