package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCardRequestJobTest {

    @Mock
    private NewCardService newCardService;

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> claimIds = List.of(uuid1, uuid2);
        given(claimRepository.getNewClaimIds()).willReturn(claimIds);

        createCardJob.executeInternal(mock(JobExecutionContext.class));

        verify(newCardService).createNewCards(claimIds);
    }
}
