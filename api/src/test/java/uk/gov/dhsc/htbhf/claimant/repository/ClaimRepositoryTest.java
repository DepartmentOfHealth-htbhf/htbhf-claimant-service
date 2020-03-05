package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import javax.validation.ConstraintViolationException;

import static com.google.common.collect.Iterables.size;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.TestConstants.MARGE_NINO;
import static uk.gov.dhsc.htbhf.TestConstants.NED_NINO;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HOMER_CLAIM_REFERENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MARGE_CLAIM_REFERENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NED_CLAIM_REFERENCE;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @AfterEach
    void afterEach() {
        claimRepository.deleteAll();
    }

    @Test
    void saveAndRetrieveClaim() {
        //Given
        Claim claim = aValidClaim();

        //When
        Claim savedClaim = claimRepository.save(claim);

        //Then
        assertThat(savedClaim.getId()).isNotNull();
        assertThat(savedClaim).isEqualTo(claim);
    }

    @Test
    void anInvalidClaimIsNotSaved() {
        //Given
        Claim invalidClaim = aClaimWithTooLongFirstName();
        //When
        Throwable thrown = catchThrowable(() -> claimRepository.save(invalidClaim));
        //Then
        assertThat(thrown).hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldHandleSqlInjectionAttempts() {
        //Given
        var sqlInjectionName = "Robert'); DROP TABLE CLAIM; --)";
        Claim sqlInjectionClaim = aClaimWithLastName(sqlInjectionName);

        //When
        claimRepository.save(sqlInjectionClaim);

        // if the sql injection was successful then findAll will fail
        //Then
        assertThat(size(claimRepository.findAll())).isEqualTo(1);
        assertThat(claimRepository.findAll().iterator().next().getClaimant().getLastName()).isEqualTo(sqlInjectionName);
    }

    @Test
    void shouldReturnEmptyListWhenNinoDoesNotExist() {
        //Given
        String nino = aValidClaimant().getNino();

        //When
        List<UUID> claimIds = claimRepository.findLiveClaimsWithNino(nino);

        //Then
        assertThat(claimIds).isEmpty();
    }

    @Test
    void shouldReturnEmptyClaimWhenReferenceDoesNotExist() {
        //Given
        String reference = aValidClaim().getReference();

        //When
        Optional<Claim> claim = claimRepository.findByReference(reference);

        //Then
        assertThat(claim).isNotPresent();
    }

    @Test
    void shouldReturnClaimWhenReferenceExist() {

        //Given
        Claim claim = aValidClaim();

        //When
        Claim savedClaim = claimRepository.save(claim);
        Optional<Claim> claimReference = claimRepository.findByReference(savedClaim.getReference());

        //Then
        assertThat(claimReference).isPresent();
    }

    @ParameterizedTest(name = "Should return id of existing claim with matching NINO when claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "ACTIVE",
            "PENDING",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimIdWhenNinoExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        List<UUID> claimIds = claimRepository.findLiveClaimsWithNino(claim.getClaimant().getNino());

        //Then
        assertThat(claimIds).isEqualTo(List.of(claim.getId()));
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when claim exists and claim status is {0}")
    @ValueSource(strings = {
            "REJECTED",
            "ERROR",
            "EXPIRED"
    })
    void shouldReturnEmptyListWhenNinoExistsOnInactiveClaim(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        List<UUID> claimIds = claimRepository.findLiveClaimsWithNino(claim.getClaimant().getNino());

        //Then
        assertThat(claimIds).isEmpty();
    }

    @Test
    void shouldReturnLiveClaimDoesNotExistWhenDwpHouseholdIdentifierDoesNotExist() {
        //Given
        Claim claim = aValidClaim();

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForDwpHousehold(claim.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when dwp household exists and the claim status is {0}")
    @ValueSource(strings = {
            "ERROR",
            "REJECTED",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenDwpHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForDwpHousehold(claim.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when dwp household exists and the claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "PENDING",
            "ACTIVE",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenDwpHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForDwpHousehold(claim.getDwpHouseholdIdentifier());

        //Then
        assertThat(claimExists).isTrue();
    }

    @Test
    void shouldReturnClaimDoesNotExistWhenHmrcHouseholdIdentifierDoesNotExist() {
        //Given
        Claim claim = aValidClaim();

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForHmrcHousehold(claim.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when hmrc household exists and the claim status is {0}")
    @ValueSource(strings = {
            "ERROR",
            "REJECTED",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenHmrcHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForHmrcHousehold(claim.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when hmrc household exists and the claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "PENDING",
            "ACTIVE",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenHmrcHouseholdIdentifierExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForHmrcHousehold(claim.getHmrcHouseholdIdentifier());

        //Then
        assertThat(claimExists).isTrue();
    }

    @Test
    void shouldReturnNoNewClaimIds() {
        //When
        List<UUID> result = claimRepository.getNewClaimIds();

        //Then
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void shouldReturnNewClaimIds() {
        //Given
        Claim newClaim = aClaimWithNinoAndClaimStatusAndReference(HOMER_NINO, ClaimStatus.NEW, NED_CLAIM_REFERENCE);
        Claim pendingClaim = aClaimWithNinoAndClaimStatusAndReference(MARGE_NINO, PENDING, HOMER_CLAIM_REFERENCE);
        claimRepository.saveAll(Arrays.asList(newClaim, pendingClaim));

        //When
        List<UUID> result = claimRepository.getNewClaimIds();

        //Then
        assertThat(result).containsOnly(newClaim.getId());
    }

    @Test
    void shouldReturnExistingClaim() {
        //Given
        Claim claim = aValidClaim();
        claimRepository.save(claim);

        //When
        Claim result = claimRepository.findClaim(claim.getId());

        //Then
        assertThat(result).isEqualTo(claim);
    }

    @Test
    void shouldReturnExistingClaims() {
        //Given
        Claim oldClaim = aValidClaimWithNinoAndRefernce(MARGE_NINO, MARGE_CLAIM_REFERENCE);
        Claim newClaim = aValidClaimWithNinoAndRefernce(HOMER_NINO, HOMER_CLAIM_REFERENCE);
        claimRepository.saveAll(List.of(oldClaim, newClaim));

        //When
        List<Claim> result = claimRepository.findAll();

        //Then
        assertThat(result).isEqualTo(List.of(oldClaim, newClaim));
    }

    @Test
    void shouldReturnEmptyListIfNoClaimsAreCreated() {

        //When
        List<Claim> result = claimRepository.findAll();

        //Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenClaimDoesNotExist() {
        //Given
        UUID claimId = UUID.randomUUID();

        //When
        Throwable thrown = catchThrowable(() -> claimRepository.findClaim(claimId));

        //Then
        assertThat(thrown).isInstanceOf(RuntimeException.class); // spring wraps the EntityNotFoundException
        assertThat(thrown.getMessage()).contains(claimId.toString());
    }

    @Test
    void shouldGetClaimsThatHaveBeenInPendingCancellationForMoreThanGivenPeriod() {
        int numberOfWeeks = 16;
        LocalDateTime now = LocalDateTime.now();
        Claim activeClaim
                = aClaimWithNinoAndCardStatusAndCardStatusTimestampAndReference(HOMER_NINO, CardStatus.ACTIVE, now.minusWeeks(17), HOMER_CLAIM_REFERENCE);
        Claim pendingClaimOlderThan16Weeks = aClaimWithNinoAndCardStatusAndCardStatusTimestampAndReference(
                MARGE_NINO,
                CardStatus.PENDING_CANCELLATION,
                now.minusWeeks(17),
                MARGE_CLAIM_REFERENCE);
        Claim pendingClaimLessThan16Weeks = aClaimWithNinoAndCardStatusAndCardStatusTimestampAndReference(
                NED_NINO,
                CardStatus.PENDING_CANCELLATION,
                now.minusWeeks(15),
                NED_CLAIM_REFERENCE);
        claimRepository.saveAll(List.of(activeClaim, pendingClaimOlderThan16Weeks, pendingClaimLessThan16Weeks));

        List<Claim> claims = claimRepository.getClaimsWithCardStatusPendingCancellationOlderThan(Period.ofWeeks(numberOfWeeks));

        assertThat(claims).containsOnly(pendingClaimOlderThan16Weeks);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoClaimsThatHaveBeenInPendingCancellationForMoreThanGivenPeriod() {
        LocalDateTime now = LocalDateTime.now();
        Claim activeClaim = aClaimWithCardStatusAndCardStatusTimestamp(CardStatus.ACTIVE, now.minusWeeks(17));
        claimRepository.save(activeClaim);

        List<Claim> claims = claimRepository.getClaimsWithCardStatusPendingCancellationOlderThan(Period.ofWeeks(16));

        assertThat(claims).isNotNull();
        assertThat(claims).isEmpty();
    }

    @Test
    void shouldReturnSingleUUIDForNino() {
        //Given
        Claim claim = aValidClaim();
        claimRepository.save(claim);

        //When
        Optional<UUID> claimId = claimRepository.findLiveClaimWithNino(HOMER_NINO);

        //Then
        assertThat(claimId).hasValue(claim.getId());
    }

    @Test
    void shouldReturnEmptyForNoMatchingNino() {
        //Given - no claims

        //When
        Optional<UUID> claimIds = claimRepository.findLiveClaimWithNino(HOMER_NINO);

        //Then
        assertThat(claimIds).isEmpty();
    }
}
