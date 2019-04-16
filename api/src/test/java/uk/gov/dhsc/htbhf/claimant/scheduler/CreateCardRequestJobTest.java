package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;
import uk.gov.dhsc.htbhf.requestcontext.RequestContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCardRequestJobTest {

    @Mock
    private NewCardService newCardService;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private RequestContext requestContext;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        UUID uuid1 = UUID.fromString("7a614d18-86a4-4ea8-8afa-fbfa9d4dd069");
        UUID uuid2 = UUID.fromString("11f75c2c-5fe1-4984-8824-283f33b22f24");
        List<UUID> claimIds = List.of(uuid1, uuid2);
        given(claimRepository.getNewClaimIds()).willReturn(claimIds);
        var execution = mock(JobExecutionContext.class);
        given(execution.getFireInstanceId()).willReturn("job-id-1");

        createCardJob.executeInternal(execution);

        verify(requestContext).setSessionId("job-id-1");
        ArgumentCaptor<UUID> argumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(newCardService, times(2)).createNewCards(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).containsOnly(uuid1, uuid2);
    }
}
