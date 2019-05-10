package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;

import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "payment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Payment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private PaymentCycle paymentCycle;

    @NotNull
    @Column(name = "card_account_id")
    private String cardAccountId;

    @NotNull
    @Column(name = "payment_amount_in_pence")
    private Integer paymentAmountInPence;

    @NotNull
    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp;

    @Column(name = "payment_reference")
    private String paymentReference;

    @NotNull
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
