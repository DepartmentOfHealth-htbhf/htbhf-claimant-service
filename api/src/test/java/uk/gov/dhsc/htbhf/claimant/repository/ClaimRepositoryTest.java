package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.validation.ConstraintViolationException;

import static com.google.common.collect.Iterables.size;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.*;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private Javers javers;

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
    void shouldReturnLiveClaimDoesNotExistWhenNinoDoesNotExist() {
        //Given
        Claim claim = aValidClaim();

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForNino(claim.getClaimant().getNino());

        //Then
        assertThat(claimExists).isFalse();
    }

    @ParameterizedTest(name = "Should return that a live claim does exist when claim exists and claim status is {0}")
    @ValueSource(strings = {
            "NEW",
            "ACTIVE",
            "PENDING",
            "PENDING_EXPIRY"
    })
    void shouldReturnLiveClaimDoesExistWhenNinoExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForNino(claim.getClaimant().getNino());

        //Then
        assertThat(claimExists).isTrue();
    }

    @ParameterizedTest(name = "Should return that a live claim does not exist when claim exists and claim status is {0}")
    @ValueSource(strings = {
            "REJECTED",
            "ERROR",
            "EXPIRED"
    })
    void shouldReturnLiveClaimDoesNotExistWhenNinoExists(ClaimStatus claimStatus) {
        //Given
        Claim claim = aClaimWithClaimStatus(claimStatus);
        claimRepository.save(claim);

        //When
        Boolean claimExists = claimRepository.liveClaimExistsForNino(claim.getClaimant().getNino());

        //Then
        assertThat(claimExists).isFalse();
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
        Claim newClaim = aClaimWithClaimStatus(ClaimStatus.NEW);
        Claim pendingClaim = aClaimWithClaimStatus(PENDING);
        claimRepository.saveAll(Arrays.asList(newClaim, pendingClaim));

        //When
        List<UUID> result = claimRepository.getNewClaimIds();

        //Then
        assertThat(result).containsOnly(newClaim.getId());
    }

    @Test
    void shouldAuditClaimUpdate() {
        //Given
        // with a versioned entity, the returned object will have an incremented version number, the object passed into save will not
        Claim claim = aClaimWithClaimStatus(ACTIVE);
        claim = claimRepository.save(claim);
        claim.setClaimStatus(PENDING);
        claim = claimRepository.save(claim);
        claim.setClaimStatus(PENDING_EXPIRY);
        claimRepository.save(claim);

        //When
        JqlQuery jqlQuery = QueryBuilder.byInstanceId(claim.getId(), Claim.class).build();
        Changes changes = javers.findChanges(jqlQuery);

        //Then
        assertThat(changes.size()).isEqualTo(2);
        assertThat(changes.get(0).toString()).isEqualTo("ValueChange{ 'claimStatus' value changed from 'PENDING' to 'PENDING_EXPIRY' }");
        assertThat(changes.get(1).toString()).isEqualTo("ValueChange{ 'claimStatus' value changed from 'ACTIVE' to 'PENDING' }");
    }

    @Test
    void shouldAuditAddressUpdate() {
        //Given
        Claim claim = aValidClaim();
        claimRepository.save(claim);
        Address address = claim.getClaimant().getAddress();
        address.setAddressLine1("test");
        claimRepository.save(claim);

        //When
        JqlQuery jqlQuery = QueryBuilder.byInstanceId(claim.getClaimant().getAddress().getId(), Address.class).build();
        Changes changes = javers.findChanges(jqlQuery);

        //Then
        assertThat(changes.size()).isEqualTo(1);
        assertThat(changes.get(0).toString()).isEqualTo("ValueChange{ 'addressLine1' value changed from 'Flat b' to 'test' }");
    }
}
