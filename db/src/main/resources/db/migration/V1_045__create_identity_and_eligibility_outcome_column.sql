alter table payment_cycle add column identity_and_eligibility_response jsonb;

update payment_cycle set identity_and_eligibility_response = '{
    "eligibilityStatus": "confirmed",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "matched",
    "emailAddressMatch": "matched",
    "addressLine1Match": "matched",
    "postcodeMatch": "matched",
    "pregnantChildDOBMatch": "not_supplied",
    "qualifyingBenefits": "universal_credit",
    "householdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "matched"
}'
    where identity_and_eligibility_response is null and qualifying_benefit_eligibility_status = 'CONFIRMED';

update payment_cycle set identity_and_eligibility_response = '{
    "eligibilityStatus": "not_confirmed",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "not_set",
    "emailAddressMatch": "not_set",
    "addressLine1Match": "not_set",
    "postcodeMatch": "not_set",
    "pregnantChildDOBMatch": "not_set",
    "qualifyingBenefits": "not_set",
    "householdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "matched"
}'
    where identity_and_eligibility_response is null and qualifying_benefit_eligibility_status = 'NOT_CONFIRMED';

update payment_cycle set identity_and_eligibility_response = '{
    "eligibilityStatus": "not_set",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "not_set",
    "emailAddressMatch": "not_set",
    "addressLine1Match": "not_set",
    "postcodeMatch": "not_set",
    "pregnantChildDOBMatch": "not_set",
    "qualifyingBenefits": "not_set",
    "householdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "not_matched"
}'
    where identity_and_eligibility_response is null and qualifying_benefit_eligibility_status = 'NOT_SET';
