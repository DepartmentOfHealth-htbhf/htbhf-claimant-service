package uk.gov.dhsc.htbhf.claimant.service.claim;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimToClaimResponseDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextType;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponseDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewClaimEvent;
import uk.gov.dhsc.htbhf.dwp.model.VerificationOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;
import uk.gov.dhsc.htbhf.reference.ReferenceGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.factory.VerificationResultFactory.buildVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision.buildWithStatus;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final EligibilityAndEntitlementService eligibilityAndEntitlementService;
    private final EventAuditor eventAuditor;
    private final ClaimMessageSender claimMessageSender;
    private final Javers javers;
    private final ClaimToClaimResponseDTOConverter claimToClaimResponseDTOConverter;

    @Value("${claim-reference.size:10}")
    private int claimReferenceSize;

    private static final Map<EligibilityStatus, ClaimStatus> STATUS_MAP = Map.of(
            EligibilityStatus.ELIGIBLE, ClaimStatus.NEW,
            EligibilityStatus.PENDING, ClaimStatus.PENDING,
            EligibilityStatus.NO_MATCH, ClaimStatus.REJECTED,
            EligibilityStatus.ERROR, ClaimStatus.ERROR,
            EligibilityStatus.DUPLICATE, ClaimStatus.REJECTED,
            EligibilityStatus.INELIGIBLE, ClaimStatus.REJECTED
    );

    public ClaimResult createClaim(ClaimRequest claimRequest, String user) {
        try {
            EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateNewClaimant(claimRequest.getClaimant(),
                    claimRequest.getEligibilityOverride());
            CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse = decision.getIdentityAndEligibilityResponse();

            if (decision.getEligibilityStatus() == EligibilityStatus.DUPLICATE) {
                Claim claim = createDuplicateClaim(claimRequest, decision, user);
                claimMessageSender.sendReportClaimMessage(claim, identityAndEligibilityResponse, ClaimAction.REJECTED);
                return ClaimResult.withNoEntitlement(claim);
            }

            VerificationResult verificationResult = buildVerificationResult(claimRequest.getClaimant(), identityAndEligibilityResponse);
            Claim claim = createNewClaim(claimRequest, decision, verificationResult, user);
            if (claim.getClaimStatus() == ClaimStatus.NEW) {
                sendMessagesForNewClaim(decision, identityAndEligibilityResponse, claim);
                VoucherEntitlement weeklyEntitlement = decision.getVoucherEntitlement().getFirstVoucherEntitlementForCycle();
                return ClaimResult.withEntitlement(claim, weeklyEntitlement, verificationResult);
            }

            if (StringUtils.isNotEmpty(claim.getClaimant().getNino())) {
                sendMessagesForRejectedClaimIfNinoPresent(identityAndEligibilityResponse, verificationResult, claim);
            }
            return ClaimResult.withNoEntitlement(claim, verificationResult);
        } catch (RuntimeException e) {
            handleFailedClaim(claimRequest, user, e);
            throw e;
        }
    }

    public List<ClaimResponseDTO> findClaims() {
        List<Claim> claims = claimRepository.findAll();
        return claimToClaimResponseDTOConverter.convert(claims);
    }

    private void sendMessagesForNewClaim(EligibilityAndEntitlementDecision decision,
                                         CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                         Claim claim) {
        if (identityAndEligibilityResponse.getEmailAddressMatch() == VerificationOutcome.MATCHED) {
            EmailType emailType = registeredChildrenContainAllDeclaredChildren(identityAndEligibilityResponse, claim)
                    ? EmailType.INSTANT_SUCCESS
                    : EmailType.INSTANT_SUCCESS_PARTIAL_CHILDREN_MATCH;
            claimMessageSender.sendInstantSuccessEmail(claim, decision, emailType);
        } else if (identityAndEligibilityResponse.getMobilePhoneMatch() == VerificationOutcome.MATCHED) {
            claimMessageSender.sendInstantSuccessText(claim, decision, TextType.INSTANT_SUCCESS_TEXT);
        } else {
            sendMessagesForPhoneOrEmailMismatch(claim, decision, identityAndEligibilityResponse);
        }

        claimMessageSender.sendNewCardMessage(claim, decision);
        claimMessageSender.sendReportClaimMessage(claim, identityAndEligibilityResponse, ClaimAction.NEW);
    }

    private void sendMessagesForPhoneOrEmailMismatch(Claim claim,
                                                     EligibilityAndEntitlementDecision decision,
                                                     CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        if (StringUtils.isNotEmpty(claim.getClaimant().getEmailAddress())) {
            claimMessageSender.sendDecisionPendingEmailMessage(claim);
        }
        LetterType letterType = registeredChildrenContainAllDeclaredChildren(identityAndEligibilityResponse, claim)
                ? LetterType.APPLICATION_SUCCESS_CHILDREN_MATCH
                : LetterType.APPLICATION_SUCCESS_CHILDREN_MISMATCH;
        claimMessageSender.sendLetterWithAddressAndPaymentFieldsMessage(claim, decision, letterType);
    }

    private void sendMessagesForRejectedClaimIfNinoPresent(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse,
                                                           VerificationResult verificationResult,
                                                           Claim claim) {
        if (verificationResult.isAddressMismatch()) {
            if (StringUtils.isNotEmpty(claim.getClaimant().getEmailAddress())) {
                claimMessageSender.sendDecisionPendingEmailMessage(claim);
            }
            claimMessageSender.sendLetterWithAddressOnlyMessage(claim, LetterType.UPDATE_YOUR_ADDRESS);
        }
        claimMessageSender.sendReportClaimMessage(claim, identityAndEligibilityResponse, ClaimAction.REJECTED);
    }

    public void updateCurrentIdentityAndEligibilityResponse(Claim claim, CombinedIdentityAndEligibilityResponse response) {
        claim.setCurrentIdentityAndEligibilityResponse(response);
        claimRepository.save(claim);
    }

    private void handleFailedClaim(ClaimRequest claimRequest, String user, RuntimeException e) {
        EligibilityAndEntitlementDecision decision = buildWithStatus(EligibilityStatus.ERROR);
        Claim claim = buildClaim(ClaimStatus.ERROR, claimRequest, decision);
        NewClaimEvent newClaimEvent = new NewClaimEvent(claim, user);
        FailureEvent failureEvent = FailureEvent.builder()
                .failureDescription("Unable to create (or update) claim")
                .failedEvent(newClaimEvent)
                .exception(e)
                .build();
        eventAuditor.auditFailedEvent(failureEvent);
        claimRepository.save(claim);
        javers.commit(user, claim);
    }

    private Claim createDuplicateClaim(ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision, String user) {
        return buildAndSaveClaim(ClaimStatus.REJECTED, claimRequest, decision, user);
    }

    private Claim createNewClaim(ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision, VerificationResult verificationResult, String user) {
        ClaimStatus claimStatus = getClaimStatus(decision.getEligibilityStatus(), verificationResult);
        return buildAndSaveClaim(claimStatus, claimRequest, decision, user);
    }

    private Claim buildAndSaveClaim(ClaimStatus claimStatus, ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision, String user) {
        Claim claim = buildClaim(claimStatus, claimRequest, decision);
        log.info("Saving new claim: {} with status {} and reference {}", claim.getId(), claim.getEligibilityStatus(), claim.getReference());
        claimRepository.save(claim);
        javers.commit(user, claim);
        eventAuditor.auditNewClaim(claim, user);
        return claim;
    }

    @SuppressFBWarnings(value = "SECMD5",
            justification = "Using a hash of the device fingerprint to identify multiple claims from the same device, not for encryption")
    private Claim buildClaim(ClaimStatus claimStatus, ClaimRequest claimRequest, EligibilityAndEntitlementDecision decision) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        Map<String, Object> deviceFingerprint = claimRequest.getDeviceFingerprint();
        String fingerprintHash = isEmpty(deviceFingerprint) ? null : DigestUtils.md5Hex(deviceFingerprint.toString());
        String reference = checkAndReturnUniqueReferenceForClaim();
        return Claim.builder()
                .dwpHouseholdIdentifier(decision.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(decision.getHmrcHouseholdIdentifier())
                .eligibilityStatus(decision.getEligibilityStatus())
                .eligibilityStatusTimestamp(currentDateTime)
                .claimStatus(claimStatus)
                .claimStatusTimestamp(currentDateTime)
                .claimant(claimRequest.getClaimant())
                .deviceFingerprint(deviceFingerprint)
                .deviceFingerprintHash(fingerprintHash)
                .webUIVersion(claimRequest.getWebUIVersion())
                .initialIdentityAndEligibilityResponse(decision.getIdentityAndEligibilityResponse())
                .currentIdentityAndEligibilityResponse(decision.getIdentityAndEligibilityResponse())
                .eligibilityOverride(claimRequest.getEligibilityOverride())
                .reference(reference)
                .build();
    }

    private String checkAndReturnUniqueReferenceForClaim() {
        String reference;
        boolean isReferenceDuplicate;
        do {
            reference = ReferenceGenerator.generateReference(claimReferenceSize);
            isReferenceDuplicate = false;
            Optional<Claim> claim = claimRepository.findByReference(reference);
            if (claim.isPresent()) {
                isReferenceDuplicate = true;
            }

        } while (isReferenceDuplicate);

        return reference;
    }

    private ClaimStatus getClaimStatus(EligibilityStatus eligibilityStatus, VerificationResult verificationResult) {
        if (verificationResult.isAddressMismatch()) {
            return ClaimStatus.REJECTED;
        } else if (verificationResult.getIsPregnantOrAtLeast1ChildMatched()) {
            return STATUS_MAP.get(eligibilityStatus);
        }
        return ClaimStatus.REJECTED;
    }

    private boolean registeredChildrenContainAllDeclaredChildren(CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse, Claim claim) {
        List<LocalDate> initiallyDeclaredChildrenDob = defaultIfNull(claim.getClaimant().getInitiallyDeclaredChildrenDob(), emptyList());
        List<LocalDate> identityAndEligibilityResponseChildren = defaultIfNull(identityAndEligibilityResponse.getDobOfChildrenUnder4(), emptyList());
        return identityAndEligibilityResponseChildren.containsAll(initiallyDeclaredChildrenDob)
                && identityAndEligibilityResponseChildren.size() >= initiallyDeclaredChildrenDob.size();
    }
}
