ALTER TABLE claim ADD CONSTRAINT unique_claimant UNIQUE (claimant_id);

ALTER TABLE claimant ADD CONSTRAINT unique_address UNIQUE (address_id);
ALTER TABLE claimant ADD FOREIGN KEY (address_id) references address;
