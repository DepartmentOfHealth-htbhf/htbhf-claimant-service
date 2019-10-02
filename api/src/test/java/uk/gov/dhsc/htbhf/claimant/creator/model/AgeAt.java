package uk.gov.dhsc.htbhf.claimant.creator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

/**
 * Use in conjunction with a duration to state when a child be that age.
 */
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public enum AgeAt {

    TODAY,
    START_OF_NEXT_CYCLE
}
