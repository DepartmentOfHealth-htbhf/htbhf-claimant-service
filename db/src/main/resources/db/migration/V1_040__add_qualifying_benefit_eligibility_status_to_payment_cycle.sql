alter table payment_cycle add column qualifying_benefit_eligibility_status varchar;

update payment_cycle set qualifying_benefit_eligibility_status = 'CONFIRMED'
    where qualifying_benefit_eligibility_status is null and eligibility_status = 'ELIGIBLE';
update payment_cycle set qualifying_benefit_eligibility_status = 'NOT_CONFIRMED'
    where qualifying_benefit_eligibility_status is null and eligibility_status = 'INELIGIBLE';