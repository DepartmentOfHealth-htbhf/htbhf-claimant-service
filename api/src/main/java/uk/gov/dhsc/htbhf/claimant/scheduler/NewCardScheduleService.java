package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class NewCardScheduleService {

    private ClaimRepository claimRepository;

    @Transactional
    public void createNewCards() {
        Stream<Claim> newClaims = claimRepository.getNewClaims();
        newClaims.forEach(this::updateClaimStatusToActive);
    }

    private void updateClaimStatusToActive(Claim claim) {
        claim.setClaimStatus(ClaimStatus.ACTIVE);
        claimRepository.save(claim);
    }
}
