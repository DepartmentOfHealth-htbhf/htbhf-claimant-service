package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "claim")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Claim extends BaseEntity {

    @NotNull
    @Column(name = "claim_status")
    @Enumerated(EnumType.STRING)
    private ClaimStatus claimStatus;

    @Column(name = "claim_status_timestamp")
    private LocalDateTime claimStatusTimestamp;

    @NotNull
    @Column(name = "eligibility_status")
    @Enumerated(EnumType.STRING)
    private EligibilityStatus eligibilityStatus;

    @Column(name = "eligibility_status_timestamp")
    private LocalDateTime eligibilityStatusTimestamp;

    @Column(name = "dwp_household_identifier")
    private String dwpHouseholdIdentifier;

    @Column(name = "hmrc_household_identifier")
    private String hmrcHouseholdIdentifier;

    @Column(name = "card_account_id")
    private String cardAccountId;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    @ToString.Exclude
    @Valid
    private Claimant claimant;
}
