package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import javax.persistence.*;

@Entity
@Table(name = "claim")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class Claim extends VersionedEntity {

    @Column(name = "claim_status")
    @Enumerated(EnumType.STRING)
    private ClaimStatus claimStatus;

    @Column(name = "claim_status_timestamp")
    private LocalDateTime claimStatusTimestamp;

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

    @Column(name = "device_fingerprint_json")
    @Type(type = "json")
    private Map<String, Object> deviceFingerprint;

    @Column(name = "device_fingerprint_hash")
    private String deviceFingerprintHash;

    @Column(name = "web_ui_version")
    private String webUIVersion;

    @OneToOne(cascade = CascadeType.ALL)
    @ToString.Exclude
    private Claimant claimant;
}
