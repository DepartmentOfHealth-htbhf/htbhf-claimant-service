package uk.gov.dhsc.htbhf.claimant.entity;

import lombok.*;
import org.hibernate.annotations.Type;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.Map;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "claim")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("PMD.TooManyFields")
public class Claim extends VersionedEntity {

    @NotNull
    @Column(name = "claim_status")
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.PRIVATE)
    private ClaimStatus claimStatus;

    @Column(name = "claim_status_timestamp")
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime claimStatusTimestamp;

    @NotNull
    @Column(name = "eligibility_override_outcome")
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.PRIVATE)
    private EligibilityOutcome eligibilityOverrideOutcome;

    @Column(name = "eligibility_status")
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.PRIVATE)
    private EligibilityStatus eligibilityStatus;

    @Column(name = "eligibility_status_timestamp")
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime eligibilityStatusTimestamp;

    @Column(name = "dwp_household_identifier")
    private String dwpHouseholdIdentifier;

    @Column(name = "hmrc_household_identifier")
    private String hmrcHouseholdIdentifier;

    @Column(name = "card_account_id")
    private String cardAccountId;

    @Column(name = "device_fingerprint_json")
    @Type(type = JSON_TYPE)
    private Map<String, Object> deviceFingerprint;

    @Column(name = "device_fingerprint_hash")
    private String deviceFingerprintHash;

    @Column(name = "web_ui_version")
    private String webUIVersion;

    @Column(name = "postcode_data_json")
    @Type(type = JSON_TYPE)
    private PostcodeData postcodeData;

    @Column(name = "card_status")
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.PRIVATE)
    private CardStatus cardStatus;

    @Column(name = "card_status_timestamp")
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime cardStatusTimestamp;

    @Column(name = "initial_identity_and_eligibility_response")
    @Type(type = JSON_TYPE)
    private CombinedIdentityAndEligibilityResponse initialIdentityAndEligibilityResponse;
    
    @Column(name = "current_identity_and_eligibility_response")
    @Type(type = JSON_TYPE)
    private CombinedIdentityAndEligibilityResponse currentIdentityAndEligibilityResponse;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL)
    @ToString.Exclude
    @Valid
    private Claimant claimant;

    public void updateClaimStatus(ClaimStatus newStatus) {
        if (claimStatus != newStatus) {
            claimStatusTimestamp = LocalDateTime.now();
        }
        claimStatus = newStatus;
    }

    public void updateEligibilityStatus(EligibilityStatus newStatus) {
        if (eligibilityStatus != newStatus) {
            eligibilityStatusTimestamp = LocalDateTime.now();
        }
        eligibilityStatus = newStatus;
    }

    public void updateCardStatus(CardStatus newStatus) {
        if (cardStatus != newStatus) {
            cardStatusTimestamp = LocalDateTime.now();
        }
        cardStatus = newStatus;
    }
}
