package uk.gov.dhsc.htbhf.claimant.creator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.creator.dwp.repository.UCHouseholdRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.EntityAgeAccelerator;


@Component
@Profile("test-claimant-creator")
@Slf4j
public class DWPMediator {

    @Autowired
    private UCHouseholdRepository ucHouseholdRepository;

    @Transactional("dwpTransactionManager")
    public void ageDatabaseEntities(int numberOfDays) {
        ucHouseholdRepository.findAll().forEach(ucHousehold -> {
            EntityAgeAccelerator.ageObject(ucHousehold, numberOfDays);
            ucHousehold.getChildren().forEach(child -> {
                EntityAgeAccelerator.ageObject(child, numberOfDays);
            });
            ucHousehold.getAdults().forEach(adult -> {
                EntityAgeAccelerator.ageObject(adult, numberOfDays);
            });
        });

    }
}
