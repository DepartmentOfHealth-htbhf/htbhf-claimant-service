
INSERT INTO claim (
    id,
    dwp_household_identifier,
    hmrc_household_identifier,
    claim_status,
    claim_status_timestamp,
    eligibility_status,
    eligibility_status_timestamp,
    claimant_id,
    created_timestamp
    )
SELECT
    id,
    dwp_household_identifier,
    hmrc_household_identifier,
    claim_status,
    created_timestamp,
    eligibility_status,
    created_timestamp,
    id,
    created_timestamp
  FROM claimant where eligibility_status is not null;

ALTER TABLE claimant DROP COLUMN eligibility_status;
ALTER TABLE claimant DROP COLUMN claim_status;
ALTER TABLE claimant DROP COLUMN dwp_household_identifier;
ALTER TABLE claimant DROP COLUMN hmrc_household_identifier;
