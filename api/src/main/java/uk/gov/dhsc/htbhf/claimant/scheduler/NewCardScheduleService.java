package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardScheduleService {

    private ClaimRepository claimRepository;

    /**
     * Print out first 10 new claims (testing purposes only).
     */
    @Transactional
    public void createNewCards() {
        Stream<Claim> newClaims = claimRepository.getNewClaims();
        newClaims.limit(10).forEach(claim ->
                log.trace(claim.getClaimant().getId().toString()));
    }
}
