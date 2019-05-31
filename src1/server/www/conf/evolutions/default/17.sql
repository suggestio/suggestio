BEGIN;

CREATE TABLE sio2.one_time_token
(
    token_id uuid NOT NULL,
    date_created timestamp without time zone NOT NULL,
    date_end timestamp without time zone NOT NULL,
    CONSTRAINT one_time_token_pkey PRIMARY KEY (token_id)
)
TABLESPACE pg_default;

ALTER TABLE sio2.one_time_token
    OWNER to postgres;
COMMENT ON TABLE sio2.one_time_token
    IS 'модель одноразовых токенов (типа uuid)';
COMMENT ON COLUMN sio2.one_time_token.date_end
    IS 'Дата, после которого токен пора удалять безвозвратно.';

COMMIT;
