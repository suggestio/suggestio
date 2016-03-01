BEGIN;



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
  OWNER TO sio2;
COMMENT ON TABLE sio2.contract
  IS 'Контракты-договоры v2. На смену bill_contract.';
COMMENT ON COLUMN sio2.contract.crand IS 'Дополнительный случайный номер договора для подавления опечаток в id-номерах договоров.';
COMMENT ON COLUMN sio2.contract.hidden_info IS 'Какая-то внутренняя информация по контракту.';
COMMENT ON COLUMN sio2.contract.suffix IS 'Суффикс номера договора.';

CREATE INDEX contract_crand_idx
  ON sio2.contract
  USING btree
  (crand);



CREATE TABLE sio2."order"
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass), -- id заказа
  status "char" NOT NULL, -- Статус обработки заказа группы item'ов.
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



CREATE TABLE sio2.item
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  order_id bigint NOT NULL,
  status "char" NOT NULL, -- статус обработки данного item'а
  type "char" NOT NULL, -- Тип субъекта сделки, для контроля данных между наследуемыми таблицами. Например, прямое размещение на узле или размещение в геотеге.
  amount double precision NOT NULL,
  currency_code character varying(3) NOT NULL,
  ad_id character varying(32) NOT NULL, -- id карточки, связанной с этим item'ом
  reason character varying(512), -- Причина отказа в размещении. Заказы в размещении карточки могут быть отменены по воле "продавца".
  rcvr_id character varying(32),
  sls character varying[], -- show levels
  gs character varying(8192), -- Geo Shape (GeoJSON), если есть.
  date_start timestamp with time zone, -- Дата-время начала действия услуги.
  date_end timestamp with time zone, -- Дата-время окончания действия услуги.
  tag_face character varying(64), -- Если покупка тега, то здесь строковое название тега. Иначе NULL.
  geo_shape character varying(65535), -- Описание связанного с item'ом гео-шейпа, если необходимо.
  date_status timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT item_pkey PRIMARY KEY (id),
  CONSTRAINT item_order_id_fkey FOREIGN KEY (order_id)
      REFERENCES sio2."order" (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT item_amount_check CHECK (amount >= 0::double precision)
)
INHERITS (sio2.gid)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.item
  OWNER TO sio2;
COMMENT ON TABLE sio2.item
  IS 'Единицы содержимого ордеров: услуги (в т.ч. размещение), товары или иные блага.';
COMMENT ON COLUMN sio2.item.status IS 'статус обработки данного item''а';
COMMENT ON COLUMN sio2.item.type IS 'Тип субъекта сделки, для контроля данных между наследуемыми таблицами. Например, прямое размещение на узле или размещение в геотеге.';
COMMENT ON COLUMN sio2.item.ad_id IS 'id карточки, связанной с этим item''ом';
COMMENT ON COLUMN sio2.item.reason IS 'Причина отказа в размещении. Заказы в размещении карточки могут быть отменены по воле "продавца".';
COMMENT ON COLUMN sio2.item.sls IS 'show levels';
COMMENT ON COLUMN sio2.item.gs IS 'Geo Shape (GeoJSON), если есть.';
COMMENT ON COLUMN sio2.item.date_start IS 'Дата-время начала действия услуги.';
COMMENT ON COLUMN sio2.item.date_end IS 'Дата-время окончания действия услуги.';
COMMENT ON COLUMN sio2.item.tag_face IS 'Если покупка тега, то здесь строковое название тега. Иначе NULL.';
COMMENT ON COLUMN sio2.item.geo_shape IS 'Описание связанного с item''ом гео-шейпа, если необходимо.';

CREATE INDEX fki_item_order_id_fkey
  ON sio2.item
  USING btree
  (order_id);



CREATE TABLE sio2.txn
(
-- Унаследована from table sio2.gid:  id bigint NOT NULL DEFAULT nextval('sio2.gid_id_seq'::regclass),
  balance_id bigint NOT NULL,
  amount double precision NOT NULL,
  date_paid timestamp with time zone, -- Дата проведения транзакции в банке, если есть. Может быть актуально для платежей по безналу.
  date_processed timestamp with time zone NOT NULL DEFAULT now(),
  comment character varying(256), -- Комментарий к платежу, если есть.
  ps_txn_uid character varying(128), -- id связанной транзакции на стороне платежной системы....
  order_id bigint, -- id ордера, с которым связана транзакция
  item_id bigint, -- id item'а, с которым связана транзакция, если есть.
  txtype "char" NOT NULL, -- Тип транзакции: платеж, возврат, рефунд и т.д.
  CONSTRAINT txn_pkey PRIMARY KEY (id),
  CONSTRAINT txn_balance_id_fkey FOREIGN KEY (balance_id)
      REFERENCES sio2.balance (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT txn_item_id_fkey FOREIGN KEY (item_id)
      REFERENCES sio2.item (id) MATCH SIMPLE
      ON UPDATE SET NULL ON DELETE SET NULL,
  CONSTRAINT txn_order_id_fkey FOREIGN KEY (order_id)
      REFERENCES sio2."order" (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
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
COMMENT ON COLUMN sio2.txn.comment IS 'Комментарий к платежу, если есть.';
COMMENT ON COLUMN sio2.txn.ps_txn_uid IS 'id связанной транзакции на стороне платежной системы.
NULL значит транзакцию без платежной системы, например просто списание бабла с баланса на какую-то операцию.';
COMMENT ON COLUMN sio2.txn.order_id IS 'id ордера, с которым связана транзакция';
COMMENT ON COLUMN sio2.txn.item_id IS 'id item''а, с которым связана транзакция, если есть.';
COMMENT ON COLUMN sio2.txn.txtype IS 'Тип транзакции: платеж, возврат, рефунд и т.д.';


CREATE INDEX fki_txn_balance_id_fkey
  ON sio2.txn
  USING btree
  (balance_id);

CREATE INDEX fki_txn_item_id_fkey
  ON sio2.txn
  USING btree
  (item_id);

CREATE INDEX fki_txn_order_id_fkey
  ON sio2.txn
  USING btree
  (order_id);


COMMIT;
