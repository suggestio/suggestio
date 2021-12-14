BEGIN;

ALTER TABLE sio2.item
    ADD COLUMN price_dsl character varying(2048);

ALTER TABLE sio2.txn
    ADD COLUMN ps_deal_id character varying(128);

CREATE INDEX txn_ps_deal_id_inx ON sio2.txn (ps_deal_id);

COMMIT;
