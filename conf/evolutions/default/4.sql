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


-- Спилить нижеперечисленные таблицы
DROP TABLE sio2.adv CASCADE;


-- Добавление adv-таблиц для описания коммерческого взаимодействия узлов ADN.
BEGIN;

CREATE TABLE sio2.adv (
  id serial NOT NULL,
  ad_id character varying(32) NOT NULL,
  amount real NOT NULL,
  currency_code character(3) NOT NULL,
  date_created timestamp with time zone NOT NULL DEFAULT now(),
  comission real, -- комиссия suggest.io за операцию.
  mode "char" NOT NULL, -- Режим текущего ряда. По сути, позволяет абстрактно разделять между собой ряды из _req, _ok и _refuse.
  on_start_page boolean NOT NULL,
  date_start timestamp with time zone NOT NULL,
  date_end timestamp with time zone NOT NULL,
  prod_adn_id character varying(32) NOT NULL,
  rcvr_adn_id character varying(32) NOT NULL,
  CHECK (amount > 0)
);
ALTER TABLE sio2.adv
  OWNER TO sio2;
COMMENT ON TABLE sio2.adv
  IS 'Абстрактная таблица для коммерческих отношений между рекламными узлами.';
COMMENT ON COLUMN sio2.adv.comission IS 'комиссия suggest.io за операцию.';
COMMENT ON COLUMN sio2.adv.mode IS 'Режим текущего ряда. По сути, позволяет абстрактно разделять между собой ряды из _req, _ok и _refuse.';


CREATE TABLE sio2.adv_ok (
  date_status timestamp with time zone NOT NULL DEFAULT now(), -- дата одобрения рекламной карточки
  prod_txn_id integer NOT NULL, -- Транзакция списания источника рекламы.
  rcvr_txn_id integer, -- id транзакции пополнения счета получателя.
  PRIMARY KEY (id),
  FOREIGN KEY (prod_txn_id)
      REFERENCES sio2.bill_txn (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  FOREIGN KEY (rcvr_txn_id)
      REFERENCES sio2.bill_txn (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CHECK (mode = 'o'::"char")
)
INHERITS (sio2.adv);
ALTER TABLE sio2.adv_ok
  OWNER TO sio2;
COMMENT ON TABLE sio2.adv_ok
  IS 'Список одобренных рекламных размещений.';
COMMENT ON COLUMN sio2.adv_ok.date_start IS 'Дата начала размещения. Если NULL, то ещё не размещено из-за другого размещения.';
COMMENT ON COLUMN sio2.adv_ok.date_status IS 'дата одобрения рекламной карточки';
COMMENT ON COLUMN sio2.adv_ok.prod_txn_id IS 'Транзакция списания источника рекламы.';
COMMENT ON COLUMN sio2.adv_ok.rcvr_txn_id IS 'id транзакции пополнения счета получателя.';


CREATE TABLE sio2.adv_refuse (
  date_status timestamp with time zone NOT NULL DEFAULT now(),
  reason character varying(4096) NOT NULL,
  PRIMARY KEY (id),
  CHECK (mode = 'e'::"char")
)
INHERITS (sio2.adv);
ALTER TABLE sio2.adv_refuse
  OWNER TO sio2;
COMMENT ON TABLE sio2.adv_refuse
  IS 'Список отказов в размещении рекламы.';


CREATE TABLE sio2.adv_req(
  prod_contract_id integer NOT NULL, -- Номер договора рекламодателя. Пока лишь на всякий случай.
  PRIMARY KEY (id),
  FOREIGN KEY (prod_contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CHECK (mode = 'r'::"char")
)
INHERITS (sio2.adv);
ALTER TABLE sio2.adv_req
  OWNER TO sio2;
COMMENT ON COLUMN sio2.adv_req.prod_contract_id IS 'Номер договора рекламодателя. Пока лишь на всякий случай.';


COMMIT;

