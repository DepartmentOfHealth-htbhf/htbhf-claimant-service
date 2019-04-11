create table claim (
    id uuid not null primary key,
    dwp_household_identifier varchar(50),
    hmrc_household_identifier varchar(50),
    claim_status varchar(50) not null,
    claim_status_timestamp TIMESTAMP not null DEFAULT NOW(),
    eligibility_status varchar(50) not null,
    eligibility_status_timestamp TIMESTAMP not null DEFAULT NOW(),
    claimant_id uuid not null REFERENCES claimant(id),
    created_timestamp TIMESTAMP not null DEFAULT NOW()
);

CREATE INDEX claim_dwp_household_identifier_idx ON claim (dwp_household_identifier);
CREATE INDEX claim_hmrc_household_identifier_idx ON claim (hmrc_household_identifier);
CREATE INDEX claim_claim_status_idx ON claim (claim_status);
