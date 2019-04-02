alter table claimant RENAME COLUMN household_identifier TO dwp_household_identifier;
alter table claimant ADD COLUMN hmrc_household_identifier varchar(50);
