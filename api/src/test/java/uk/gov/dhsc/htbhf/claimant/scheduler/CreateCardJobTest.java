package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCardJobTest {

    @Mock
    private NewCardService newCardService;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        createCardJob.executeInternal(mock(JobExecutionContext.class));

        verify(newCardService).createNewCards();
    }
}
