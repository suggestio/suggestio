BEGIN;

ALTER TABLE sio2.bill_balance
  ADD CHECK (amount >= overdraft);

ALTER TABLE sio2.bill_balance
  ADD CHECK (blocked >= 0);


-- Добавить колонку для задания комиссионных по договору.
ALTER TABLE sio2.bill_contract
   ADD COLUMN sio_comission real NOT NULL DEFAULT 0.30;
COMMENT ON COLUMN sio2.bill_contract.sio_comission
  IS 'комиссия s.io на прибыль за размещение рекламы';
ALTER TABLE sio2.bill_contract
   ALTER COLUMN sio_comission DROP DEFAULT;


ALTER TABLE sio2.adv_ok
  ADD COLUMN online boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN sio2.adv_ok.online IS 'Удовлетворён ли реквест размещение системой sio?';

COMMIT;



BEGIN;

ALTER TABLE sio2.bill_mmp_daily
  ADD COLUMN on_start_page real NOT NULL DEFAULT 3;
COMMENT ON COLUMN sio2.bill_mmp_daily.on_start_page IS 'Мультипликатор цены при активации галочки "на главном экране".';
ALTER TABLE sio2.bill_mmp_daily
   ALTER COLUMN on_start_page DROP DEFAULT;

COMMIT;
