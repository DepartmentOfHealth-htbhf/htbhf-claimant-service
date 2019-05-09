package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.converter.ListOfLocalDatesConverter;
import uk.gov.dhsc.htbhf.claimant.entity.converter.PaymentCycleVoucherEntitlementConverter;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
public class PaymentCycle extends BaseEntity {

    @Version
    @Column(name = "version_number")
    private Integer versionNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Claim claim;

    @NotNull
    @Column(name = "card_account_id")
    private String cardAccountId;

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    private final Set<Payment> payments = new HashSet<>();

    @Column(name = "cycle_start_date")
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date")
    private LocalDate cycleEndDate;

    @Column(name = "eligibility_status")
    @Enumerated(EnumType.STRING)
    private EligibilityStatus eligibilityStatus;

    @Column(name = "voucher_entitlement_json")
    @Convert(converter = PaymentCycleVoucherEntitlementConverter.class)
    private PaymentCycleVoucherEntitlement voucherEntitlement;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "children_dob_json")
    @Convert(converter = ListOfLocalDatesConverter.class)
    private List<LocalDate> childrenDob;

    @Column(name = "total_vouchers")
    private Integer totalVouchers;

    @Column(name = "total_entitlement_amount_in_pence")
    private Integer totalEntitlementAmountInPence;

    @Column(name = "card_balance_in_pence")
    private Integer cardBalanceInPence;

    @Column(name = "card_balance_timestamp")
    private LocalDateTime cardBalanceTimestamp;

    public PaymentCycle addPayment(Payment payment) {
        this.payments.add(payment);
        return this;
    }

    public Set<Payment> getPayments() {
        return unmodifiableSet(payments);
    }
}
