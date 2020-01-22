alter table claim
    ADD COLUMN first_name varchar,
    ADD COLUMN last_name varchar,
    ADD COLUMN nino varchar,
    ADD COLUMN date_of_birth date,
    ADD COLUMN expected_delivery_date date,
    ADD COLUMN address_id uuid,
    ADD COLUMN phone_number varchar,
    ADD COLUMN email_address varchar,
    ADD COLUMN initially_declared_children_dob jsonb;

alter table claim add constraint fk_claim_address foreign key (address_id) references address(id);
alter table claim add constraint claim_unique_address UNIQUE (address_id);
alter table claim alter column claimant_id drop not null;

CREATE INDEX claim_nino_idx ON claim (nino);

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