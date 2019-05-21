CREATE TABLE jv_commit
(
    commit_pk bigint NOT NULL,
    author character varying(200),
    commit_date timestamp,
    commit_date_instant character varying(30),
    commit_id numeric(22,2),
    CONSTRAINT jv_commit_pk PRIMARY KEY (commit_pk)
);

CREATE INDEX jv_commit_commit_id_idx ON jv_commit(commit_id);

CREATE TABLE jv_commit_property
(
    property_name character varying(191) NOT NULL,
    property_value character varying(600),
    commit_fk bigint NOT NULL,
    CONSTRAINT jv_commit_property_pk PRIMARY KEY (commit_fk, property_name),
    CONSTRAINT jv_commit_property_commit_fk FOREIGN KEY (commit_fk)
        REFERENCES jv_commit (commit_pk) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE INDEX jv_commit_property_commit_fk_idx ON jv_commit_property(commit_fk);

CREATE INDEX jv_commit_property_property_name_property_value_idx ON jv_commit_property (property_name, property_value);

CREATE TABLE jv_global_id
(
    global_id_pk bigint NOT NULL,
    local_id character varying(191),
    fragment character varying(200),
    type_name character varying(200),
    owner_id_fk bigint,
    CONSTRAINT jv_global_id_pk PRIMARY KEY (global_id_pk),
    CONSTRAINT jv_global_id_owner_id_fk FOREIGN KEY (owner_id_fk)
        REFERENCES jv_global_id (global_id_pk) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE INDEX jv_global_id_local_id_idx ON jv_global_id(local_id);

CREATE INDEX jv_global_id_owner_id_fk_idx ON jv_global_id(owner_id_fk);

CREATE TABLE jv_snapshot
(
    snapshot_pk bigint NOT NULL,
    type character varying(200),
    version bigint,
    state text,
    changed_properties text,
    managed_type character varying(200),
    global_id_fk bigint,
    commit_fk bigint,
    CONSTRAINT jv_snapshot_pk PRIMARY KEY (snapshot_pk),
    CONSTRAINT jv_snapshot_commit_fk FOREIGN KEY (commit_fk)
        REFERENCES jv_commit (commit_pk) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT jv_snapshot_global_id_fk FOREIGN KEY (global_id_fk)
        REFERENCES jv_global_id (global_id_pk) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE INDEX jv_snapshot_commit_fk_idx ON jv_snapshot(commit_fk);
CREATE INDEX jv_snapshot_global_id_fk_idx ON jv_snapshot(global_id_fk);

CREATE SEQUENCE jv_commit_pk_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE jv_global_id_pk_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE SEQUENCE public.jv_snapshot_pk_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
