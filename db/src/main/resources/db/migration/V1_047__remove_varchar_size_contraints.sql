-- Remove size constraints from all varchar columns where they are specified
ALTER TABLE address ALTER COLUMN address_line_1 TYPE varchar;
ALTER TABLE address ALTER COLUMN address_line_2 TYPE varchar;
ALTER TABLE address ALTER COLUMN town_or_city TYPE varchar;
ALTER TABLE address ALTER COLUMN postcode TYPE varchar;
ALTER TABLE address ALTER COLUMN county TYPE varchar;

ALTER TABLE claim ALTER COLUMN dwp_household_identifier TYPE varchar;
ALTER TABLE claim ALTER COLUMN hmrc_household_identifier TYPE varchar;
ALTER TABLE claim ALTER COLUMN claim_status TYPE varchar;
ALTER TABLE claim ALTER COLUMN eligibility_status TYPE varchar;
ALTER TABLE claim ALTER COLUMN card_account_id TYPE varchar;
ALTER TABLE claim ALTER COLUMN device_fingerprint_hash TYPE varchar;
ALTER TABLE claim ALTER COLUMN web_ui_version TYPE varchar;
ALTER TABLE claim ALTER COLUMN card_status TYPE varchar;

ALTER TABLE claimant ALTER COLUMN first_name TYPE varchar;
ALTER TABLE claimant ALTER COLUMN last_name TYPE varchar;
ALTER TABLE claimant ALTER COLUMN nino TYPE varchar;
ALTER TABLE claimant ALTER COLUMN phone_number TYPE varchar;
ALTER TABLE claimant ALTER COLUMN email_address TYPE varchar;

ALTER TABLE message_queue ALTER COLUMN message_type TYPE varchar;
ALTER TABLE message_queue ALTER COLUMN status TYPE varchar;

ALTER TABLE payment ALTER COLUMN card_account_id TYPE varchar;
ALTER TABLE payment ALTER COLUMN payment_reference TYPE varchar;
ALTER TABLE payment ALTER COLUMN payment_status TYPE varchar;

ALTER TABLE payment_cycle ALTER COLUMN eligibility_status TYPE varchar;
ALTER TABLE payment_cycle ALTER COLUMN payment_cycle_status TYPE varchar;
