package uk.gov.dhsc.htbhf.claimant.reporting;

/**
 * Action that triggered a payment event.
 */
public enum PaymentAction {
    INITIAL_PAYMENT, // initial payment when a new claim is created
    SCHEDULED_PAYMENT, // regular payment made as part of a payment cycle
    TOP_UP_PAYMENT, // payment made when a user updates their claim with a pregnancy and we to pay pregnancy vouchers
    MANUAL_PAYMENT // manual payment made by helpdesk user
}
