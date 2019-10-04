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
import uk.gov.dhsc.htbhf.claimant.creator.model.ChildInfo;
import uk.gov.dhsc.htbhf.claimant.creator.model.ClaimantInfo;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy;

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
    private PaymentRepository paymentRepository;
    private AddressDTOToAddressConverter addressDTOToAddressConverter;

    @PostConstruct
    @Transactional
    public void loadClaimantIntoDatabase() throws IOException {
        ClaimantInfo claimantInfo = objectMapper.readValue(new ClassPathResource("test-claimant-creator/claimant.yml").getFile(), ClaimantInfo.class);
        log.info("Saving claim {}", claimantInfo);
        String dwpHouseholdIdentifier = createDWPHousehold(claimantInfo);
        Claim claim = createActiveClaim(claimantInfo, dwpHouseholdIdentifier);
        PaymentCycle paymentCycle = createPaymentCycleEndingYesterday(claim, claimantInfo.getChildrenInfo());
        createPayment(claim, paymentCycle);
    }

    private String createDWPHousehold(ClaimantInfo claimantInfo) {
        Stream<UCHousehold> existingHouseholds = ucHouseholdRepository.findAllHouseholdsByAdultWithNino(claimantInfo.getNino());
        ucHouseholdRepository.deleteAll(existingHouseholds.collect(Collectors.toList()));

        String dwpHouseholdIdentifier = UUID.randomUUID().toString();
        UCHousehold ucHousehold = UCHousehold.builder().householdIdentifier(dwpHouseholdIdentifier).build();

        UCAdult ucAdult = createUCAdult(claimantInfo, ucHousehold);
        ucHousehold.addAdult(ucAdult);

        Set<UCChild> ucChildren = createUCChildren(claimantInfo.getChildrenInfo());
        ucHousehold.setChildren(ucChildren);

        ucHouseholdRepository.save(ucHousehold);
        return dwpHouseholdIdentifier;
    }

    private Set<UCChild> createUCChildren(List<ChildInfo> childrenAgeInfo) {
        return childrenAgeInfo.stream()
                .map(this::convertChildAgeInfoToUCChild)
                .collect(Collectors.toSet());
    }

    private UCChild convertChildAgeInfoToUCChild(ChildInfo childInfo) {
        return UCChild.builder()
                .dateOfBirth(convertChildAgeInfoToDate(childInfo))
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

    private Claim createActiveClaim(ClaimantInfo claimantInfo, String dwpHouseholdIdentifier) {
        Address address = addressDTOToAddressConverter.convert(claimantInfo.getAddressDTO());
        Claimant claimant = createClaimant(claimantInfo, address);
        Claim claim = createClaim(dwpHouseholdIdentifier, claimant);
        return claimRepository.save(claim);
    }

    private Claim createClaim(String dwpHouseholdIdentifier, Claimant claimant) {
        return Claim.builder()
                .claimStatus(ClaimStatus.ACTIVE)
                .claimStatusTimestamp(LocalDateTime.now())
                .cardAccountId(UUID.randomUUID().toString())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .dwpHouseholdIdentifier(dwpHouseholdIdentifier)
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
                .childrenDob(createListOfChildrenDatesOfBirth(claimantInfo.getChildrenInfo()))
                .build();
    }

    private PaymentCycle createPaymentCycleEndingYesterday(Claim claim, List<ChildInfo> childrenAgeInfo) {
        LocalDate cycleStartDate = LocalDate.now().minusDays(28);
        List<LocalDate> childrenDatesOfBirth = createListOfChildrenDatesOfBirth(childrenAgeInfo);
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
                cycleStartDate,
                childrenDatesOfBirth,
                claim.getClaimant().getExpectedDeliveryDate());
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .cycleStartDate(cycleStartDate)
                .cycleEndDate(LocalDate.now().minusDays(1))
                .claim(claim)
                .paymentCycleStatus(PaymentCycleStatus.FULL_PAYMENT_MADE)
                .voucherEntitlement(voucherEntitlement)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .childrenDob(childrenDatesOfBirth)
                .build();
        return paymentCycleRepository.save(paymentCycle);
    }

    private void createPayment(Claim claim, PaymentCycle paymentCycle) {
        Payment payment = Payment.builder()
                .paymentAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .cardAccountId(claim.getCardAccountId())
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentReference(UUID.randomUUID().toString())
                .paymentCycle(paymentCycle)
                .paymentTimestamp(LocalDateTime.now().minusDays(28))
                .claim(claim)
                .build();
        paymentRepository.save(payment);
    }

    private List<LocalDate> createListOfChildrenDatesOfBirth(List<ChildInfo> childrenInfo) {
        return childrenInfo.stream()
                .filter(childInfo -> !childInfo.isExcludeFromExistingCycle())
                .map(this::convertChildAgeInfoToDate)
                .collect(Collectors.toList());
    }

    private LocalDate convertChildAgeInfoToDate(ChildInfo childInfo) {
        LocalDate childDateOfBirth = LocalDate.now().minus(childInfo.getAge());
        return childInfo.getAt() == AgeAt.START_OF_NEXT_CYCLE ? childDateOfBirth.plusDays(28) : childDateOfBirth;
    }
}
