package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
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
    @Column(name = "claim_id")
    private UUID claimId;

    @NotNull
    @Column(name = "card_account_id")
    private String cardAccountId;

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
}
