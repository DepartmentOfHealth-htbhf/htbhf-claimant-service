package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static java.util.Collections.unmodifiableSet;

@Entity
@Table(name = "payment_cycle")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("PMD.TooManyFields")
public class PaymentCycle extends BaseEntity {

    @NotNull
    @Version
    @Column(name = "version_number")
    private Integer versionNumber;

    @NotNull
    @OneToOne
    private Claim claim;

    @NotNull
    @Column(name = "card_account_id")
    private String cardAccountId;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    private final Set<Payment> payments = new HashSet<>();

    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date")
    private LocalDate cycleEndDate;

    @NotNull
    @Column(name = "eligibility_status")
    @Enumerated(EnumType.STRING)
    private EligibilityStatus eligibilityStatus;

    @NotNull
    @Column(name = "voucher_entitlement_json")
    // TODO change type to json
    private String voucherEntitlementJson;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "children_dob_json")
    // TODO change type to json
    private String childrenDobJson;

    @NotNull
    @Column(name = "total_vouchers")
    private Integer totalVouchers;

    @NotNull
    @Column(name = "total_entitlement_amount_in_pence")
    private Integer totalEntitlementAmountInPence;

    @NotNull
    @Column(name = "card_balance_in_pence")
    private Integer cardBalanceInPence;

    @NotNull
    @Column(name = "card_balance_timestamp")
    private LocalDateTime cardBalanceTimestamp;

    @NotNull
    @Column(name = "payment_amount_in_pence")
    private Integer paymentAmountInPence;

    @NotNull
    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp;

    @NotNull
    @Column(name = "payment_reference")
    private String paymentReference;

    @NotNull
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;


    public PaymentCycle addPayment(Payment payment) {
        this.payments.add(payment);
        return this;
    }

    public Set<Payment> getPayments() {
        return unmodifiableSet(payments);
    }
}