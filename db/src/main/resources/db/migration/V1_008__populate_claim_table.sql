
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

ALTER TABLE claimant ALTER COLUMN eligibility_status DROP NOT NULL;
ALTER TABLE claimant ALTER COLUMN claim_status DROP NOT NULL;

update claimant set claim_status = null, eligibility_status = null;
