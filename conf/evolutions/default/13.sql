-- billing v2.


CREATE TABLE sio2.gid
(
  id bigserial NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.gid
  OWNER TO sio2;
COMMENT ON TABLE sio2.gid
  IS 'Таблица глобальных целочисленных id для различных подтаблиц.';




CREATE TABLE sio2.contract
(
  -- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  date_created timestamp with time zone NOT NULL DEFAULT now(),
  crand integer NOT NULL, -- Дополнительный случайный номер договора для подавления опечаток в id-номерах договоров.
  hidden_info character varying(1024), -- Какая-то внутренняя информация по контракту.
  suffix character varying(8), -- Суффикс номера договора.
  CONSTRAINT contract_pkey PRIMARY KEY (id)
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.contract
  OWNER TO postgres;
COMMENT ON TABLE sio2.contract
  IS 'Контракты-договоры v2. На смену bill_contract.';
COMMENT ON COLUMN sio2.contract.crand IS 'Дополнительный случайный номер договора для подавления опечаток в id-номерах договоров.';
COMMENT ON COLUMN sio2.contract.hidden_info IS 'Какая-то внутренняя информация по контракту.';
COMMENT ON COLUMN sio2.contract.suffix IS 'Суффикс номера договора.';


CREATE INDEX contract_crand_idx
  ON sio2.contract
  USING btree
  (crand);




CREATE TABLE sio2.balance
(
  -- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  contract_id bigint NOT NULL, -- id контракта
  amount double precision NOT NULL,
  currency_code character varying(3) NOT NULL,
  blocked double precision NOT NULL DEFAULT 0,
  low double precision, -- Нижний кредитный лимит, NULL значит 0. Проверяется на клиенте, когда это необходимо.
  CONSTRAINT balance_pkey PRIMARY KEY (id),
  CONSTRAINT balance_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.contract (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT balance_contract_id_currency_code_key UNIQUE (contract_id, currency_code)
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.balance
  OWNER TO sio2;
COMMENT ON TABLE sio2.balance
  IS 'Балансы на счетах/депозитах v2. Привязана к контракту.';
COMMENT ON COLUMN sio2.balance.id IS 'ключ для списка транзакций по остаткам на счете';
COMMENT ON COLUMN sio2.balance.contract_id IS 'id контракта';
COMMENT ON COLUMN sio2.balance.low IS 'Нижний кредитный лимит, NULL значит 0. Проверяется на клиенте, когда это необходимо.';

CREATE INDEX balance_contract_id_idx
  ON sio2.balance
  USING btree
  (contract_id);



CREATE TABLE sio2."order"
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass), -- id заказа
  status "char" NOT NULL, -- Статус обработки заказа группы item'ов.
  amount double precision NOT NULL, -- вся цена заказа, рассчитанная и оплаченная в у.е.
  currency_code character varying(3) NOT NULL, -- валюта заказа (у.е.)
  contract_id bigint NOT NULL,
  date_created timestamp with time zone NOT NULL DEFAULT now(), -- Дата создания ордера.
  date_status timestamp with time zone NOT NULL DEFAULT now(), -- Дата выставления последнего статуса.
  CONSTRAINT order_pkey PRIMARY KEY (id),
  CONSTRAINT order_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.contract (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2."order"
  OWNER TO sio2;
COMMENT ON TABLE sio2."order"
  IS 'Заказы, т.е. группы item''ов, оплачиваемые пачкой.';
COMMENT ON COLUMN sio2."order".id IS 'id заказа';
COMMENT ON COLUMN sio2."order".status IS 'Статус обработки заказа группы item''ов.';
COMMENT ON COLUMN sio2."order".amount IS 'вся цена заказа, рассчитанная и оплаченная в у.е.';
COMMENT ON COLUMN sio2."order".currency_code IS 'валюта заказа (у.е.)';
COMMENT ON COLUMN sio2."order".date_created IS 'Дата создания ордера.';
COMMENT ON COLUMN sio2."order".date_status IS 'Дата выставления последнего статуса.';

CREATE INDEX fki_order_contract_id_fkey
  ON sio2."order"
  USING btree
  (contract_id);

CREATE INDEX order_status_idx
  ON sio2."order"
  USING btree
  (status);



CREATE TABLE sio2.txn
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  balance_id bigint NOT NULL,
  amount double precision NOT NULL,
  date_paid timestamp with time zone, -- Дата проведения транзакции в банке, если есть. Может быть актуально для платежей по безналу.
  date_processed timestamp with time zone NOT NULL DEFAULT now(),
  payment_comment character varying(256), -- Комментарий к платежу, если есть.
  ps_txn_uid character varying(128), -- id связанной транзакции на стороне платежной системы....
  order_id bigint, -- id ордера, по которому проходит пополнение/списание....
  CONSTRAINT txn_pkey PRIMARY KEY (id),
  CONSTRAINT txn_balance_id_fkey FOREIGN KEY (balance_id)
      REFERENCES sio2.balance (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT txn_order_id_fkey FOREIGN KEY (order_id)
      REFERENCES sio2."order" (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT txn_ps_txn_uid_key UNIQUE (ps_txn_uid)
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.txn
  OWNER TO sio2;
COMMENT ON TABLE sio2.txn
  IS 'Таблица транзакций на балансах.';
COMMENT ON COLUMN sio2.txn.date_paid IS 'Дата проведения транзакции в банке, если есть. Может быть актуально для платежей по безналу.';
COMMENT ON COLUMN sio2.txn.payment_comment IS 'Комментарий к платежу, если есть.';
COMMENT ON COLUMN sio2.txn.ps_txn_uid IS 'id связанной транзакции на стороне платежной системы.
NULL значит транзакцию без платежной системы, например просто списание бабла с баланса на какую-то операцию.';
COMMENT ON COLUMN sio2.txn.order_id IS 'id ордера, по которому проходит пополнение/списание.
NULL значит какое-то действие без ордера, например внезапное внесение на депозит по безналу.';

CREATE INDEX fki_txn_balance_id_fkey
  ON sio2.txn
  USING btree
  (balance_id);

CREATE INDEX fki_txn_order_id_fkey
  ON sio2.txn
  USING btree
  (order_id);




CREATE TABLE sio2.item
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  order_id bigint NOT NULL,
  status "char" NOT NULL, -- статус обработки данного item'а
  type "char" NOT NULL, -- Тип субъекта сделки, для контроля данных между наследуемыми таблицами. Например, прямое размещение на узле или размещение в геотеге.
  amount double precision NOT NULL,
  currency_code character varying(3) NOT NULL
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.item
  OWNER TO sio2;
COMMENT ON TABLE sio2.item
  IS 'Абстрактное содержимое ордера';
COMMENT ON COLUMN sio2.item.status IS 'статус обработки данного item''а';
COMMENT ON COLUMN sio2.item.type IS 'Тип субъекта сделки, для контроля данных между наследуемыми таблицами. Например, прямое размещение на узле или размещение в геотеге.';



CREATE TABLE sio2.item_adv
(
-- Унаследована from table sio2.item:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
-- Унаследована from table sio2.item:  order_id bigint NOT NULL,
-- Унаследована from table sio2.item:  status "char" NOT NULL,
-- Унаследована from table sio2.item:  type "char" NOT NULL,
-- Унаследована from table sio2.item:  amount double precision NOT NULL,
-- Унаследована from table sio2.item:  currency_code character varying(3) NOT NULL,
  date_start timestamp with time zone NOT NULL,
  date_end timestamp with time zone NOT NULL,
  ad_id character varying(32) NOT NULL, -- id размещаемой карточки.
  reason character varying(512), -- Причина отказа в размещении. Заказы в размещении карточки могут быть отменены по воле "продавца".
  CONSTRAINT item_adv_check CHECK (date_start < date_end)
)
INHERITS (sio2.item)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.item_adv
  OWNER TO sio2;
COMMENT ON TABLE sio2.item_adv
  IS 'Элемент ордера, связанный с размещением карточки.';
COMMENT ON COLUMN sio2.item_adv.ad_id IS 'id размещаемой карточки.';
COMMENT ON COLUMN sio2.item_adv.reason IS 'Причина отказа в размещении. Заказы в размещении карточки могут быть отменены по воле "продавца".';



CREATE TABLE sio2.item_adv_direct
(
-- Унаследована from table sio2.item_adv:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
-- Унаследована from table sio2.item_adv:  order_id bigint NOT NULL,
-- Унаследована from table sio2.item_adv:  status "char" NOT NULL,
-- Унаследована from table sio2.item_adv:  type "char" NOT NULL,
-- Унаследована from table sio2.item_adv:  amount double precision NOT NULL,
-- Унаследована from table sio2.item_adv:  currency_code character varying(3) NOT NULL,
-- Унаследована from table sio2.item_adv:  date_start timestamp with time zone NOT NULL,
-- Унаследована from table sio2.item_adv:  date_end timestamp with time zone NOT NULL,
-- Унаследована from table sio2.item_adv:  ad_id character varying(32) NOT NULL,
-- Унаследована from table sio2.item_adv:  reason character varying(512),
  rcvr_id character varying(32) NOT NULL,
  sls character varying[] NOT NULL, -- show levels
  CONSTRAINT item_adv_direct_order_id_fkey FOREIGN KEY (order_id)
      REFERENCES sio2."order" (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT item_adv_check CHECK (date_start < date_end),
  CONSTRAINT item_adv_direct_type_check CHECK (type = 'a'::"char")
)
INHERITS (sio2.item_adv)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.item_adv_direct
  OWNER TO sio2;
COMMENT ON TABLE sio2.item_adv_direct
  IS 'Таблица прямых размещений карточек на узлах. Пришла на смену bill_adv таблиц, теперь как частный случай заказа.';
COMMENT ON COLUMN sio2.item_adv_direct.sls IS 'show levels';

CREATE INDEX fki_item_adv_direct_order_id_fkey
  ON sio2.item_adv_direct
  USING btree
  (order_id);

CREATE INDEX item_adv_direct_rcvr_id_idx
  ON sio2.item_adv_direct
  USING btree
  (rcvr_id COLLATE pg_catalog."default");

CREATE INDEX item_adv_direct_status_idx
  ON sio2.item_adv_direct
  USING btree
  (status);
COMMENT ON INDEX sio2.item_adv_direct_status_idx
  IS 'Индекс нужен для поиска необработанных рядов по статусу.';

