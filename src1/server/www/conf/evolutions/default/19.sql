BEGIN;

ALTER TABLE sio2.txn
    ADD COLUMN metadata character varying(1024);

ALTER TABLE sio2.txn
    ADD COLUMN pay_system character varying(32);

COMMIT;
