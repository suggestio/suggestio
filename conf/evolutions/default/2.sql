--DROP TABLE sio2.bill_tariff_fee;
--DROP TABLE sio2.bill_tariff;


BEGIN;


ALTER TABLE sio2.bill_balance
   ADD COLUMN overdraft real NOT NULL DEFAULT 0;
COMMENT ON COLUMN sio2.bill_balance.overdraft
  IS 'Допустимый овердрафт на балансе. По сути - нижнее значение amount.';


CREATE TABLE sio2.bill_tariff(
  id serial NOT NULL,
  contract_id integer NOT NULL, -- id договора
  name character varying(64) NOT NULL, -- некое отображаемое имя
  ttype "char" NOT NULL, -- тип тарифа
  is_enabled boolean NOT NULL,
  date_first timestamp(0) with time zone NOT NULL DEFAULT now(),
  date_created timestamp with time zone NOT NULL DEFAULT now(),
  date_modified timestamp with time zone,
  date_last timestamp with time zone, -- Последние выполнение тарифа.
  tinterval interval NOT NULL, -- интервал снятия тарифа
  date_status timestamp with time zone NOT NULL DEFAULT now(),
  generation integer NOT NULL DEFAULT 0,
  debit_count integer NOT NULL DEFAULT 0 -- счетчик списаний по этому тарифу
);
ALTER TABLE sio2.bill_tariff
  OWNER TO sio2;
COMMENT ON COLUMN sio2.bill_tariff.contract_id IS 'id договора';
COMMENT ON COLUMN sio2.bill_tariff.name IS 'некое отображаемое имя';
COMMENT ON COLUMN sio2.bill_tariff.ttype IS 'тип тарифа';
COMMENT ON COLUMN sio2.bill_tariff.date_last IS 'Последние выполнение тарифа.';
COMMENT ON COLUMN sio2.bill_tariff.tinterval IS 'интервал снятия тарифа';
COMMENT ON COLUMN sio2.bill_tariff.debit_count IS 'счетчик списаний по этому тарифу';



CREATE TABLE sio2.bill_tariff_fee (
  fee real NOT NULL, -- Сколько платить (тариф).
  fee_cc character(3) NOT NULL, -- код валюты цены, указанной fee. "cc" сокр. "currency code".
  CONSTRAINT bill_tariff_fee_pkey PRIMARY KEY (id),
  CONSTRAINT bill_tariff_fee_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT bill_tariff_fee_ttype_check CHECK (ttype = 'f'::"char")
)
INHERITS (sio2.bill_tariff);
ALTER TABLE sio2.bill_tariff_fee
  OWNER TO sio2;
COMMENT ON COLUMN sio2.bill_tariff_fee.fee IS 'Сколько платить (тариф).';
COMMENT ON COLUMN sio2.bill_tariff_fee.fee_cc IS 'код валюты цены, указанной fee. "cc" сокр. "currency code".';


-- Index: sio2.fki_bill_tariff_fee_contract_id_fkey

-- DROP INDEX sio2.fki_bill_tariff_fee_contract_id_fkey;

CREATE INDEX fki_bill_tariff_fee_contract_id_fkey
  ON sio2.bill_tariff_fee
  USING btree
  (contract_id);


COMMIT;

