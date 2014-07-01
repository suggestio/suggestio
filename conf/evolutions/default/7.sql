-- 2014.07.01: внесены изменения в родительскую таблицу до ввода модели в эксплуатацию.

-- DROP TABLE sio2.bill_pay_reqs CASCADE;

BEGIN;


CREATE TABLE sio2.bill_pay_reqs
(
  id serial NOT NULL,
  contract_id integer NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.bill_pay_reqs
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_pay_reqs
  IS 'Таблица-каталог банковских реквизитов.';



CREATE TABLE sio2.bill_pay_reqs_ru
(
-- Унаследована from table sio2.bill_pay_reqs:  id integer NOT NULL DEFAULT nextval('sio2.bill_pay_reqs_id_seq'::regclass),
  r_name character varying(128) NOT NULL, -- Имя получателя платежа.
  r_inn bigint NOT NULL, -- ИНН получателя платежа.
  r_kpp bigint NOT NULL, -- КПП получателя платежа.
  r_okato bigint, -- ОКАТО получателя платежа. deprecated by ОКТМО.
  r_oktmo bigint, -- ОКТМО получателя платежа, пришел на смену ОКАТО в 2005 г.
  bank_name character varying(128) NOT NULL, -- название банка-получателя.
  bank_bik bigint NOT NULL, -- БИК банка получателя.
  bank_kbk character varying(64) NOT NULL, -- КБК-код банка получателя. Обычно состоит из цифр и пробелов.
  account_number character varying(32) NOT NULL, -- Номер счета получателя в банке получателя. Обычно состоит из 20 цифр.
  comment_prefix character varying(128),
  comment_suffix character varying(128),
-- Унаследована from table :  contract_id integer NOT NULL,
  CONSTRAINT bill_pay_reqs_ru_pkey PRIMARY KEY (id),
  CONSTRAINT bill_pay_reqs_ru_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT bill_pay_reqs_ru_check CHECK (NOT r_okato IS NULL OR NOT r_oktmo IS NULL)
)
INHERITS (sio2.bill_pay_reqs)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.bill_pay_reqs_ru
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_pay_reqs_ru
  IS 'Таблица платежных реквизитов для налоговых резидентов РФ.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.r_name IS 'Имя получателя платежа.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.r_inn IS 'ИНН получателя платежа.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.r_kpp IS 'КПП получателя платежа.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.r_okato IS 'ОКАТО получателя платежа. deprecated by ОКТМО.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.r_oktmo IS 'ОКТМО получателя платежа, пришел на смену ОКАТО в 2005 г.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.bank_name IS 'название банка-получателя.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.bank_bik IS 'БИК банка получателя.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.bank_kbk IS 'КБК-код банка получателя. Обычно состоит из цифр и пробелов.';
COMMENT ON COLUMN sio2.bill_pay_reqs_ru.account_number IS 'Номер счета получателя в банке получателя. Обычно состоит из 20 цифр.';

COMMIT;
