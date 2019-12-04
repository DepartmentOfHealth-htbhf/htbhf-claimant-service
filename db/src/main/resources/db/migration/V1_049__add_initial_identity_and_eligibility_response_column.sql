alter table claim add column initial_identity_and_eligibility_response jsonb;

update claim set initial_identity_and_eligibility_response = '{
    "eligibilityStatus": "confirmed",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "matched",
    "emailAddressMatch": "matched",
    "addressLine1Match": "matched",
    "postcodeMatch": "matched",
    "pregnantChildDOBMatch": "not_supplied",
    "qualifyingBenefits": "universal_credit",
    "dwpHouseholdIdentifier": "",
    "hmrcHouseholdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "matched"
}'
    where initial_identity_and_eligibility_response is null and eligibility_status = 'ELIGIBLE';

update claim set initial_identity_and_eligibility_response = '{
    "eligibilityStatus": "not_confirmed",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "not_set",
    "emailAddressMatch": "not_set",
    "addressLine1Match": "not_set",
    "postcodeMatch": "not_set",
    "pregnantChildDOBMatch": "not_set",
    "qualifyingBenefits": "not_set",
    "dwpHouseholdIdentifier": "",
    "hmrcHouseholdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "matched"
}'
    where initial_identity_and_eligibility_response is null and eligibility_status = 'INELIGIBLE';

update claim set initial_identity_and_eligibility_response = '{
    "eligibilityStatus": "not_set",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "not_set",
    "emailAddressMatch": "not_set",
    "addressLine1Match": "not_set",
    "postcodeMatch": "not_set",
    "pregnantChildDOBMatch": "not_set",
    "qualifyingBenefits": "not_set",
    "dwpHouseholdIdentifier": "",
    "hmrcHouseholdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "not_matched"
}'
    where initial_identity_and_eligibility_response is null and eligibility_status = 'NO_MATCH';

update claim set initial_identity_and_eligibility_response = '{
    "eligibilityStatus": "confirmed",
    "deathVerificationFlag": "n/a",
    "mobilePhoneMatch": "matched",
    "emailAddressMatch": "matched",
    "addressLine1Match": "matched",
    "postcodeMatch": "matched",
    "pregnantChildDOBMatch": "not_supplied",
    "qualifyingBenefits": "universal_credit",
    "dwpHouseholdIdentifier": "",
    "hmrcHouseholdIdentifier": "",
    "dobOfChildrenUnder4": [],
    "identityStatus": "matched"
}'
    where initial_identity_and_eligibility_response is null and eligibility_status = 'DUPLICATE';
