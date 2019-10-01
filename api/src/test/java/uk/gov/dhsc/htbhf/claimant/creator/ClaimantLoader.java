package uk.gov.dhsc.htbhf.claimant.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.converter.AddressDTOToAddressConverter;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc.UCAdult;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc.UCChild;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.entities.uc.UCHousehold;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.repository.UCHouseholdRepository;
import uk.gov.dhsc.htbhf.claimant.creator.model.AgeAt;
import uk.gov.dhsc.htbhf.claimant.creator.model.ChildAgeInfo;
import uk.gov.dhsc.htbhf.claimant.creator.model.ClaimantInfo;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import static java.util.Collections.nCopies;

/**
 * Populates the claimant and DWP database with the data in a {@link ClaimantInfo} object.
 */
@Component
@AllArgsConstructor
@Slf4j
@Profile("test-claimant-creator")
public class ClaimantLoader {

    private ObjectMapper objectMapper;
    private ClaimRepository claimRepository;
    private UCHouseholdRepository ucHouseholdRepository;
    private PaymentCycleRepository paymentCycleRepository;
    private AddressDTOToAddressConverter addressDTOToAddressConverter;

    @PostConstruct
    public void loadClaimantIntoDatabase() throws IOException {
        ClaimantInfo claimantInfo = objectMapper.readValue(new ClassPathResource("test-claimant-creator/claimant.yml").getFile(), ClaimantInfo.class);
        log.info("Saving claim {}", claimantInfo);
        String dwpHouseholdIdentifier = createDWPHousehold(claimantInfo);
        Claim claim = createActiveClaim(claimantInfo, dwpHouseholdIdentifier);
        createPaymentCycleEndingYesterday(claim);
    }

    private String createDWPHousehold(ClaimantInfo claimantInfo) {
        String dwpHouseholdIdentifier = UUID.randomUUID().toString();
        UCHousehold ucHousehold = UCHousehold.builder().householdIdentifier(dwpHouseholdIdentifier).build();

        UCAdult ucAdult = createUCAdult(claimantInfo, ucHousehold);
        ucHousehold.addAdult(ucAdult);

        Set<UCChild> ucChildren = createUCChildren(claimantInfo.getChildrenAgeInfo());
        ucHousehold.setChildren(ucChildren);

        ucHouseholdRepository.save(ucHousehold);
        return dwpHouseholdIdentifier;
    }

    private Set<UCChild> createUCChildren(List<ChildAgeInfo> childrenAgeInfo) {
        return childrenAgeInfo.stream()
                .map(this::convertChildAgeInfoToUCChild)
                .collect(Collectors.toSet());
    }

    private UCChild convertChildAgeInfoToUCChild(ChildAgeInfo childAgeInfo) {
        return UCChild.builder()
                .dateOfBirth(convertChildAgeInfoToDate(childAgeInfo))
                .build();
    }

    private UCAdult createUCAdult(ClaimantInfo claimantInfo, UCHousehold ucHousehold) {
        return UCAdult.builder()
                .household(ucHousehold)
                .addressLine1(claimantInfo.getAddressDTO().getAddressLine1())
                .addressLine2(claimantInfo.getAddressDTO().getAddressLine2())
                .townOrCity(claimantInfo.getAddressDTO().getTownOrCity())
                .postcode(claimantInfo.getAddressDTO().getPostcode())
                .forename(claimantInfo.getFirstName())
                .surname(claimantInfo.getLastName())
                .nino(claimantInfo.getNino())
                .build();
    }

    private Claim createActiveClaim(ClaimantInfo claimantInfo, String dipHouseholdIdentifier) {
        Address address = addressDTOToAddressConverter.convert(claimantInfo.getAddressDTO());
        Claimant claimant = createClaimant(claimantInfo, address);
        Claim claim = createClaim(dipHouseholdIdentifier, claimant);
        return claimRepository.save(claim);
    }

    private Claim createClaim(String dipHouseholdIdentifier, Claimant claimant) {
        return Claim.builder()
                .claimStatus(ClaimStatus.ACTIVE)
                .claimStatusTimestamp(LocalDateTime.now())
                .cardAccountId(UUID.randomUUID().toString())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .dwpHouseholdIdentifier(dipHouseholdIdentifier)
                .claimant(claimant)
                .build();
    }

    private Claimant createClaimant(ClaimantInfo claimantInfo, Address address) {
        return Claimant.builder()
                .address(address)
                .dateOfBirth(claimantInfo.getDateOfBirth())
                .emailAddress(claimantInfo.getEmailAddress())
                .phoneNumber(claimantInfo.getMobile())
                .firstName(claimantInfo.getFirstName())
                .lastName(claimantInfo.getLastName())
                .nino(claimantInfo.getNino())
                .expectedDeliveryDate(claimantInfo.getExpectedDeliveryDate())
                .childrenDob(createListOfChildrenDatesOfBirth(claimantInfo.getChildrenAgeInfo()))
                .build();
    }

    private void createPaymentCycleEndingYesterday(Claim claim) {
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .cycleStartDate(LocalDate.now())
                .cycleEndDate(LocalDate.now().minusDays(1))
                .claim(claim)
                .paymentCycleStatus(PaymentCycleStatus.NEW)
                .voucherEntitlement(createEmptyPaymentCycleVoucherEntitlement()) // null voucher entitlement will cause errors
                .build();
        paymentCycleRepository.save(paymentCycle);
    }

    private PaymentCycleVoucherEntitlement createEmptyPaymentCycleVoucherEntitlement() {
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(nCopies(4, VoucherEntitlement.builder().build()))
                .build();
    }

    private List<LocalDate> createListOfChildrenDatesOfBirth(List<ChildAgeInfo> childAgeInfo) {
        return childAgeInfo.stream()
                .map(this::convertChildAgeInfoToDate)
                .collect(Collectors.toList());
    }

    private LocalDate convertChildAgeInfoToDate(ChildAgeInfo childAgeInfo) {
        LocalDate childDateOfBirth = LocalDate.now().minus(childAgeInfo.getAge());
        return childAgeInfo.getAt() == AgeAt.START_OF_NEXT_CYCLE ? childDateOfBirth.plusDays(28) : childDateOfBirth;
    }
}
