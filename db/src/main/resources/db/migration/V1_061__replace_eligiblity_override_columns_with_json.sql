UPDATE claim AS c1
SET eligibility_override = (
  SELECT row_to_json(t) FROM (
    SELECT eligibility_override_outcome as "eligibilityOutcome",
           eligibility_override_until as "overrideUntil",
           eligibility_override_children_dob as "childrenDob"
    FROM claim AS c2 WHERE c1.id = c2.id and eligibility_override_outcome is not null
        and eligibility_override is null
  ) t
)::jsonb;

alter table claim drop column eligibility_override_outcome;
alter table claim drop column eligibility_override_until;
alter table claim drop column eligibility_override_children_dob;