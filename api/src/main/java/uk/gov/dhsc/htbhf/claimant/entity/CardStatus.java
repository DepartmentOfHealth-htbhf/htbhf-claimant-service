package uk.gov.dhsc.htbhf.claimant.entity;

/**
 * The possible values for a status of a card.
 */
public enum CardStatus {
    /**
     * The card has been created and is not scheduled to be cancelled.
     */
    ACTIVE,
    /**
     * The card is going to be cancelled in 16 weeks time unless the status is changed back to ACTIVE.
     */
    PENDING_CANCELLATION,
    /**
     * The card will be cancelled in one weeks time.
     */
    SCHEDULED_FOR_CANCELLATION,
    /**
     * The card have been cancelled and can no longer be used.
     */
    CANCELLED
}
