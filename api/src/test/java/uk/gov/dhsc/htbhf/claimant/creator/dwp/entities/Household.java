package uk.gov.dhsc.htbhf.claimant.creator.dwp.entities;

import java.util.Set;

public interface Household {

    String getHouseholdIdentifier();

    Set<? extends Child> getChildren();
}
