package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    private ClaimRepository claimRepository;

    /**
     * Print out first 10 new claims (testing purposes only).
     */
    @Transactional
    public void createNewCards() {
        claimRepository.getNewClaims().limit(10).forEach(
                claim -> log.info("Creating card for new claim {}", claim.getId())
        );
    }
}
