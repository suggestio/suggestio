BEGIN;

ALTER TABLE sio2.bill_txn DROP CONSTRAINT bill_txn_contract_id_fkey;


ALTER TABLE sio2.bill_mmp_daily DROP CONSTRAINT bill_mmp_daily_contract_id_fkey;

ALTER TABLE sio2.bill_mmp_daily
  ADD CONSTRAINT bill_mmp_daily_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT;

COMMIT;

