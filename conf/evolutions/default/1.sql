CREATE SCHEMA sio2
  AUTHORIZATION sio2;


BEGIN;


CREATE TABLE sio2.bill_contract (
  id serial NOT NULL, -- Порядковый номер договора
  crand integer NOT NULL DEFAULT round(random() * 1000),
  adn_id character varying(32) NOT NULL, -- es id узла, с которым заключен контракт
  contract_date date NOT NULL, -- дата заключения договора
  date_created timestamp with time zone NOT NULL DEFAULT now(), -- дата создания этого ряда
  hidden_info text, -- какие-то особые отметки, отображаемые только операторам
  is_active boolean NOT NULL DEFAULT true,
  suffix character varying(16), -- опциональный суффикс id договора
  PRIMARY KEY (id)
);
ALTER TABLE sio2.bill_contract
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_contract
  IS 'Контракты (договоры)';
COMMENT ON COLUMN sio2.bill_contract.id IS 'Порядковый номер договора';
COMMENT ON COLUMN sio2.bill_contract.adn_id IS 'es id узла, с которым заключен контракт';
COMMENT ON COLUMN sio2.bill_contract.contract_date IS 'дата заключения договора';
COMMENT ON COLUMN sio2.bill_contract.date_created IS 'дата создания этого ряда';
COMMENT ON COLUMN sio2.bill_contract.hidden_info IS 'какие-то особые отметки, отображаемые только операторам';
COMMENT ON COLUMN sio2.bill_contract.suffix IS 'опциональный суффикс id договора';


CREATE INDEX contract_adn_id_idx
  ON sio2.bill_contract
  USING btree
  (adn_id COLLATE pg_catalog."default");




CREATE TABLE sio2.bill_balance (
  adn_id character varying(32) NOT NULL, -- id узла рекламной сети
  amount real NOT NULL, -- текущее состояние счета
  currency character(3),
  PRIMARY KEY (adn_id)
);
ALTER TABLE sio2.bill_balance
  OWNER TO sio2;
COMMENT ON COLUMN sio2.bill_balance.adn_id IS 'id узла рекламной сети';
COMMENT ON COLUMN sio2.bill_balance.amount IS 'текущее состояние счета';




CREATE TABLE sio2.bill_txn (
  id serial NOT NULL,
  contract_id integer NOT NULL, -- id контракта
  amount real NOT NULL,
  currency character(3), -- Код валюты. Если NULL, то считаем что "RUB".
  date_paid timestamp with time zone NOT NULL, -- Дата-время начала операции. Т.е. когда квитанцию получили в банке-источнике, но ещё не обработали.
  date_processed timestamp with time zone NOT NULL DEFAULT now(),
  payment_comment character varying(256) NOT NULL, -- Исходный комментарий к платежу.
  txn_uid character varying(128) NOT NULL, -- уникальный id транзакции по мнению банка
  PRIMARY KEY (id),
  FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
);
ALTER TABLE sio2.bill_txn
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_txn
  IS 'транзакции';
COMMENT ON COLUMN sio2.bill_txn.contract_id IS 'id контракта';
COMMENT ON COLUMN sio2.bill_txn.currency IS 'Код валюты. Если NULL, то считаем что "RUB".';
COMMENT ON COLUMN sio2.bill_txn.date_paid IS 'Дата-время начала операции. Т.е. когда квитанцию получили в банке-источнике, но ещё не обработали.';
COMMENT ON COLUMN sio2.bill_txn.payment_comment IS 'Исходный комментарий к платежу.';
COMMENT ON COLUMN sio2.bill_txn.txn_uid IS 'уникальный id транзакции по мнению банка';


CREATE INDEX fki_bill_txn_contract_id_fkey
  ON sio2.bill_txn
  USING btree
  (contract_id);

ALTER TABLE sio2.bill_txn
  ADD CONSTRAINT bill_txn_txn_uid_contract_id_key UNIQUE(txn_uid, contract_id);


COMMIT;
