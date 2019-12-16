package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationResultTest {

    @Test
    void shouldHaveNoAddressMismatchForAllMatch() {
        VerificationResult result = VerificationResultTestDataFactory.anAllMatchedVerificationResult();
        assertThat(result.isAddressMismatch()).isFalse();
    }

    @Test
    void shouldHaveNoAddressMismatchForIdentityNotMatched() {
        VerificationResult result = VerificationResultTestDataFactory.aNothingMatchedVerificationResult();
        assertThat(result.isAddressMismatch()).isFalse();
    }

    @Test
    void shouldHaveAddressMismatchForPostcodeMismatch() {
        VerificationResult result = VerificationResultTestDataFactory.aPostcodeMismatchVerificationResult();
        assertThat(result.isAddressMismatch()).isTrue();
    }

    @Test
    void shouldHaveAddressMismatchForAddressLine1Mismatch() {
        VerificationResult result = VerificationResultTestDataFactory.anAddressLine1MismatchVerificationResult();
        assertThat(result.isAddressMismatch()).isTrue();
    }

    @Test
    void shouldHaveAddressMismatchForFullAddressMismatch() {
        VerificationResult result = VerificationResultTestDataFactory.aFullAddressMismatchVerificationResult();
        assertThat(result.isAddressMismatch()).isTrue();
    }

    @Test
    void shouldHaveNoAddressMismatchForEmptyResult() {
        VerificationResult result = VerificationResult.builder().build();
        assertThat(result.isAddressMismatch()).isFalse();
    }
}
