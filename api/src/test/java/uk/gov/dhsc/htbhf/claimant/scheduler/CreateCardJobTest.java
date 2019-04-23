package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCardJobTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private NewCardService newCardService;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCreateNewCards() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        given(claimRepository.getNewClaimIds()).willReturn(List.of(uuid1, uuid2));

        createCardJob.createNewCards();

        verify(claimRepository).getNewClaimIds();
        ArgumentCaptor<UUID> argumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(newCardService, times(2)).createNewCard(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).containsOnly(uuid1, uuid2);
    }
}
