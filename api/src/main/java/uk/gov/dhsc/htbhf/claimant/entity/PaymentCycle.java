package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import org.hibernate.annotations.Type;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
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
public class PaymentCycle extends VersionedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Claim claim;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_cycle_id")
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
    @Type(type = "json")
    private PaymentCycleVoucherEntitlement voucherEntitlement;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "children_dob_json")
    @Type(type = "json")
    private List<LocalDate> childrenDob; // not used in code, but useful for MI and helpdesk

    @Column(name = "total_vouchers")
    private Integer totalVouchers;

    @Column(name = "total_entitlement_amount_in_pence")
    private Integer totalEntitlementAmountInPence;

    @Column(name = "card_balance_in_pence")
    private Integer cardBalanceInPence;

    @Column(name = "card_balance_timestamp")
    private LocalDateTime cardBalanceTimestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_cycle_status")
    private PaymentCycleStatus paymentCycleStatus;

    public PaymentCycle addPayment(Payment payment) {
        this.payments.add(payment);
        payment.setPaymentCycle(this);
        return this;
    }

    public Set<Payment> getPayments() {
        return unmodifiableSet(payments);
    }

    /**
     * Sets the voucherEntitlement and updates values that depend on it (totalVouchers, totalEntitlementAmountInPence).
     * @param voucherEntitlement the entitlement to apply.
     */
    public void applyVoucherEntitlement(PaymentCycleVoucherEntitlement voucherEntitlement) {
        this.setVoucherEntitlement(voucherEntitlement);
        if (voucherEntitlement == null) {
            this.setTotalEntitlementAmountInPence(null);
            this.setTotalVouchers(null);
        } else {
            this.setTotalEntitlementAmountInPence(voucherEntitlement.getTotalVoucherValueInPence());
            this.setTotalVouchers(voucherEntitlement.getTotalVoucherEntitlement());
        }
    }
}
