package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class CreateCardJobTest {

    @Mock
    private NewCardService newCardService;
    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private CreateCardJob createCardJob;

    @Test
    void shouldCallService() {
        Claim claim = aValidClaim();
        given(claimRepository.getNewClaims()).willReturn(Stream.of(claim));

        createCardJob.executeInternal(mock(JobExecutionContext.class));

        verify(newCardService).createNewCard(claim);
    }
}
