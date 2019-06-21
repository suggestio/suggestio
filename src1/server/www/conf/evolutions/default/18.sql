BEGIN;

ALTER TABLE sio2.one_time_token
    ADD COLUMN info character varying(8192);

COMMENT ON COLUMN sio2.one_time_token.info
    IS 'Какие-то произвольные неиндексируемые данные для хранимого токена.';

COMMIT;
