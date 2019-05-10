package uk.gov.dhsc.htbhf.claimant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.entitlement.CycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.ChildDTO;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimAuditor;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.min;
import static java.util.stream.Collectors.toList;
import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildNewCardMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityService eligibilityService;
    private final CycleEntitlementCalculator cycleEntitlementCalculator;
    private final ClaimAuditor claimAuditor;
    private final MessageQueueDAO messageQueueDAO;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public ClaimResult createClaim(Claimant claimant) {
        try {
            EligibilityResponse eligibilityResponse = eligibilityService.determineEligibility(claimant);
            Claim claim = createAndSaveClaim(claimant, eligibilityResponse);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                PaymentCycleVoucherEntitlement voucherEntitlement = getEntitlement(claim, eligibilityResponse);
                sendNewCardMessage(claim, voucherEntitlement);
                return createResult(claim, voucherEntitlement);
            }
            return createResult(claim);
        } catch (RuntimeException e) {
            createAndSaveClaim(claimant, buildWithStatus(EligibilityStatus.ERROR));
            throw e;
        }
    }

    private Claim createAndSaveClaim(Claimant claimant, EligibilityResponse eligibilityResponse) {
        Claim claim = buildClaim(claimant, eligibilityResponse);
        claimRepository.save(claim);
        log.info("Saved new claimant: {} with status {}", claim.getId(), claim.getEligibilityStatus());
        claimAuditor.auditNewClaim(claim);
        return claim;
    }

    private void sendNewCardMessage(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        NewCardRequestMessagePayload payload = buildNewCardMessagePayload(claim, voucherEntitlement);
        messageQueueDAO.sendMessage(payload, CREATE_NEW_CARD);
    }

    private Claim buildClaim(Claimant claimant, EligibilityResponse eligibilityResponse) {
        ClaimStatus claimStatus = STATUS_MAP.get(eligibilityResponse.getEligibilityStatus());
        LocalDateTime currentDateTime = LocalDateTime.now();
        return Claim.builder()
                .dwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier())
                .eligibilityStatus(eligibilityResponse.getEligibilityStatus())
                .eligibilityStatusTimestamp(currentDateTime)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(currentDateTime)
                .claimant(claimant)
                .build();
    }

    private ClaimResult createResult(Claim claim) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .build();
    }

    private ClaimResult createResult(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        VoucherEntitlement firstVoucherEntitlement
                = min(voucherEntitlement.getVoucherEntitlements(), Comparator.comparing(VoucherEntitlement::getEntitlementDate));

        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(firstVoucherEntitlement))
                .build();
    }

    private PaymentCycleVoucherEntitlement getEntitlement(Claim claim, EligibilityResponse eligibilityResponse) {
        return cycleEntitlementCalculator.calculateEntitlement(
                Optional.ofNullable(claim.getClaimant().getExpectedDeliveryDate()),
                getDateOfBirthOfChildren(eligibilityResponse),
                LocalDate.now());
    }

    private List<LocalDate> getDateOfBirthOfChildren(EligibilityResponse eligibilityResponse) {
        return eligibilityResponse.getChildren().stream()
                .map(ChildDTO::getDateOfBirth)
                .collect(toList());
    }
}
