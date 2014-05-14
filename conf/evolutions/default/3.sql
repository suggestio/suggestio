BEGIN;

-- Подготовка к тарификации по просмотрам/кликам:
-- - перенос колонки bill_tariff.tinteval в bill_tariff_fee.tinterval

ALTER TABLE sio2.bill_tariff_fee
  ADD COLUMN tinterval1 interval;
COMMENT ON COLUMN sio2.bill_tariff_fee.tinterval1 IS 'интервал снятия тарифа';

UPDATE sio2.bill_tariff_fee SET tinterval1 = tinterval;
ALTER TABLE sio2.bill_tariff_fee ALTER COLUMN tinterval1 SET NOT NULL;
ALTER TABLE sio2.bill_tariff DROP COLUMN tinterval;

ALTER TABLE sio2.bill_tariff_fee RENAME tinterval1 TO tinterval;


-- Создать таблицу для тарифов, ориентированных на точечных списаний по просмотрам/переходам и т.д.
CREATE TABLE sio2.bill_tariff_stat (
-- Унаследована from table sio2.bill_tariff:  id integer NOT NULL DEFAULT nextval('sio2.bill_tariff_id_seq'::regclass),
-- Унаследована from table sio2.bill_tariff:  contract_id integer NOT NULL,
-- Унаследована from table sio2.bill_tariff:  name character varying(64) NOT NULL,
-- Унаследована from table sio2.bill_tariff:  ttype "char" NOT NULL,
-- Унаследована from table sio2.bill_tariff:  is_enabled boolean NOT NULL,
-- Унаследована from table sio2.bill_tariff:  date_first timestamp(0) with time zone NOT NULL DEFAULT now(),
-- Унаследована from table sio2.bill_tariff:  date_created timestamp with time zone NOT NULL DEFAULT now(),
-- Унаследована from table sio2.bill_tariff:  date_modified timestamp with time zone,
-- Унаследована from table sio2.bill_tariff:  date_last timestamp with time zone,
-- Унаследована from table sio2.bill_tariff:  date_status timestamp with time zone NOT NULL DEFAULT now(),
-- Унаследована from table sio2.bill_tariff:  generation integer NOT NULL DEFAULT 0,
-- Унаследована from table sio2.bill_tariff:  debit_count integer NOT NULL DEFAULT 0,
  debited_total real NOT NULL DEFAULT 0, -- Общий объём списаний по этому тарифу.
  debit_for "char" NOT NULL, -- За что происходит списание: click (c), view (v).
  debit_amount real NOT NULL,
  currency_code character(3) NOT NULL,
  CONSTRAINT bill_tariff_stat_pkey PRIMARY KEY (id),
  CONSTRAINT bill_tariff_stat_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT bill_tariff_stat_ttype_check CHECK (ttype = 's'::"char")
)
INHERITS (sio2.bill_tariff);
ALTER TABLE sio2.bill_tariff_stat
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_tariff_stat
  IS 'Таблица данных по списанию денег по статистике просмотров.';
COMMENT ON COLUMN sio2.bill_tariff_stat.debited_total IS 'Общий объём списаний по этому тарифу.';
COMMENT ON COLUMN sio2.bill_tariff_stat.debit_for IS 'За что происходит списание: click (c), view (v).';
COMMENT ON COLUMN sio2.bill_tariff_stat.debit_amount IS 'Объем единоразового списания (цена за клик/просмотр).';

-- DROP INDEX sio2.fki_bill_tariff_stat_contract_id_fkey;
CREATE INDEX fki_bill_tariff_stat_contract_id_fkey
  ON sio2.bill_tariff_stat
  USING btree
  (contract_id);

COMMIT;
