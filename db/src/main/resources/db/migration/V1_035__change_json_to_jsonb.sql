ALTER TABLE payment_cycle
  ALTER COLUMN voucher_entitlement_json
  SET DATA TYPE jsonb
  USING voucher_entitlement_json::jsonb;

ALTER TABLE payment_cycle
  ALTER COLUMN children_dob_json
  SET DATA TYPE jsonb
  USING children_dob_json::jsonb;

ALTER TABLE claim
  ALTER COLUMN device_fingerprint_json
  SET DATA TYPE jsonb
  USING device_fingerprint_json::jsonb;

ALTER TABLE claimant
  ALTER COLUMN children_dob_json
  SET DATA TYPE jsonb
  USING children_dob_json::jsonb;
