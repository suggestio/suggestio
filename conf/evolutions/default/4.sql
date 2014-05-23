-- Балансу требуется заблокированный объём средств.
-- Списку транзакций - колонки для идентификации связанных объектов-покупок.

BEGIN;

-- bill_balance: нужна колонка для заблокированных денег.
ALTER TABLE sio2.bill_balance
  ADD COLUMN blocked real  NOT NULL  DEFAULT 0;
COMMENT ON COLUMN sio2.bill_balance.blocked
  IS 'Заблокированные на счете средства. Т.е. тратить их нельзя, но в случае отказа в покупке они возвращаются назад на счет.';


-- bill_txn: нужно линковать рекламную карточку
ALTER TABLE sio2.bill_txn
   ADD COLUMN ad_id character varying(32);
COMMENT ON COLUMN sio2.bill_txn.ad_id
  IS 'Опциональный id рекламной карточки, с которой связана транзакция.';

-- bill_txn: нужна комиссия suggest.io за операцию оплаты.
ALTER TABLE sio2.bill_txn
  ADD COLUMN comission_pc real;
COMMENT ON COLUMN sio2.bill_txn.comission_pc
  IS 'Необязательная комиссия за операцию в процентах от суммы транзакции.';


COMMIT;
