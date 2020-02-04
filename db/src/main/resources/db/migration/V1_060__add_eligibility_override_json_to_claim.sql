alter table claim ADD COLUMN IF NOT EXISTS eligibility_override json;

UPDATE claim AS c1
SET eligibility_override = (
  SELECT row_to_json(t) FROM (
    SELECT eligibility_override_outcome as "eligibilityOutcome",
           eligibility_override_until as "overrideUntil",
           eligibility_override_children_dob as "childrenDob"
    FROM claim AS c2 WHERE c1.id = c2.id and eligibility_override_outcome is not null
  ) t
)::jsonb;
