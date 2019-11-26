package uk.gov.dhsc.htbhf.claimant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

public class ClaimantServiceAssertionUtils {

    public static final URI CLAIMANT_ENDPOINT_URI = URI.create("/v2/claims");
    public static final DateTimeFormatter EMAIL_DATE_PATTERN = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    private static final ThreadLocal<DecimalFormat> CURRENCY_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("£#,#0.00"));

    public static void assertClaimantMatchesClaimantDTO(ClaimantDTO claimant, Claimant persistedClaim) {
        assertThat(persistedClaim.getNino()).isEqualTo(claimant.getNino());
        assertThat(persistedClaim.getFirstName()).isEqualTo(claimant.getFirstName());
        assertThat(persistedClaim.getLastName()).isEqualTo(claimant.getLastName());
        assertThat(persistedClaim.getDateOfBirth()).isEqualTo(claimant.getDateOfBirth());
        assertThat(persistedClaim.getExpectedDeliveryDate()).isEqualTo(claimant.getExpectedDeliveryDate());
        assertAddressEqual(persistedClaim.getAddress(), claimant.getAddress());
    }

    public static RequestEntity buildClaimRequestEntity(Object requestObject) {
        return buildClaimRequestEntityForUri(requestObject, CLAIMANT_ENDPOINT_URI);
    }

    public static RequestEntity buildClaimRequestEntityForUri(Object requestObject, URI uri) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new RequestEntity<>(requestObject, headers, HttpMethod.POST, uri);
    }

    public static String formatVoucherAmount(int voucherCount) {
        return (voucherCount == 0) ? "" : CURRENCY_FORMAT.get().format(new BigDecimal(voucherCount * VOUCHER_VALUE_IN_PENCE).divide(ONE_HUNDRED));
    }

    public static List<Payment> getPaymentsWithStatus(PaymentCycle paymentCycle, PaymentStatus status) {
        return paymentCycle.getPayments().stream().filter(p -> p.getPaymentStatus() == status).collect(Collectors.toList());
    }

    public static void assertThatPaymentCycleHasFailedPayments(PaymentCycle paymentCycle, int expectedFailureCount) {
        assertThat(paymentCycle.getPayments()).isNotEmpty();
        List<Payment> failedPayments = getPaymentsWithStatus(paymentCycle, PaymentStatus.FAILURE);
        assertThat(failedPayments).hasSize(expectedFailureCount);
    }

    private static void assertAddressEqual(Address actual, AddressDTO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
        assertThat(actual.getAddressLine2()).isEqualTo(expected.getAddressLine2());
        assertThat(actual.getTownOrCity()).isEqualTo(expected.getTownOrCity());
        assertThat(actual.getCounty()).isEqualTo(expected.getCounty());
        assertThat(actual.getPostcode()).isEqualTo(expected.getPostcode());
    }
}
