CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

CREATE TABLE urls
(
    id         UUID NOT NULL,
    short_code VARCHAR(14),
    long_url   TEXT,
    CONSTRAINT pk_urls PRIMARY KEY (id)
);

ALTER TABLE urls
    ADD CONSTRAINT uc_urls_long_url UNIQUE (long_url);

ALTER TABLE urls
    ADD CONSTRAINT uc_urls_short_code UNIQUE (short_code);

CREATE INDEX idx_short_code ON urls (short_code);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);