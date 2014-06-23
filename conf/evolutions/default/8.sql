-- Возникла необходимость создать бесплатное размещение.
-- Удаляем ограничения adv-таблиц, мешающие этому действу.

BEGIN;

ALTER TABLE sio2.adv DROP CONSTRAINT adv_amount_check;

ALTER TABLE sio2.adv_ok
   ALTER COLUMN prod_txn_id DROP NOT NULL;

ALTER TABLE sio2.adv_ok
   ADD COLUMN is_partner boolean NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN sio2.adv_ok.is_partner
  IS 'размещена ли эта рекламная карточка по партёрской программе, т.е. в обход модерации?';


COMMIT;
