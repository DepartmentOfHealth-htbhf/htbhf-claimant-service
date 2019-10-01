package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.*;

import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "payment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private PaymentCycle paymentCycle;

    @Column(name = "card_account_id")
    private String cardAccountId;

    @Column(name = "payment_amount_in_pence")
    private Integer paymentAmountInPence;

    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
