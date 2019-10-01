package uk.gov.dhsc.htbhf.claimant.creator.entities.dwp;

import java.util.Set;

public interface Household {

    String getHouseholdIdentifier();

    Set<? extends Child> getChildren();
}
