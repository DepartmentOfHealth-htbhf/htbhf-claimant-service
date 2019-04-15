package uk.gov.dhsc.htbhf.claimant.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardService {

    /**
     * Print out first 10 new claims (testing purposes only).
     */
    @Transactional
    public void createNewCard(Claim claim) {
        log.info("Creating new card for claim {}", claim.getId());
    }
}
