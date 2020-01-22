update claim  set
  first_name = cm.first_name,
  last_name = cm.last_name,
  nino = cm.nino,
  date_of_birth = cm.date_of_birth,
  expected_delivery_date = cm.expected_delivery_date,
  address_id = cm.address_id,
  phone_number = cm.phone_number,
  email_address = cm.email_address,
  initially_declared_children_dob = cm.initially_declared_children_dob
from claimant cm
where claimant_id = cm.id;

alter table claim drop column claimant_id;
drop table claimant;