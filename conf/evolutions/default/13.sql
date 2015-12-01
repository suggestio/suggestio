-- 2015.nov.26. Нужна новая унифицированная и расширябельная модель платных размещений.


-- Корневая таблица. Колонки объявляются только в ней. Остальные таблицы молча наследуют их без изменений.
CREATE TABLE sio2.adv2
(
      id serial NOT NULL,
      mode "char" NOT NULL,
      ad_id character varying(32) NOT NULL,
      date_start timestamp with time zone NOT NULL,
      date_end timestamp with time zone NOT NULL,
      producer_id character varying(32) NOT NULL,
      prod_contract_id integer NOT NULL,
      amount real NOT NULL,
      currency_code character varying(3) NOT NULL,
      date_created timestamp with time zone NOT NULL DEFAULT now(),
      date_status timestamp with time zone NOT NULL DEFAULT now(),
      reason character varying(512),
      rcvr_id character varying(32),
      show_levels character varying[] NOT NULL DEFAULT '{}'::character varying[],
      CONSTRAINT adv2_pkey PRIMARY KEY (id)
)
WITH (
      OIDS=FALSE
);
ALTER TABLE sio2.adv2
  OWNER TO sio2;
COMMENT ON TABLE sio2.adv2
  IS 'Корневая таблица платных размещений второго поколения, более абстрактна и унифицирована.
Размещение adv v1 является лишь частным случаем, задаваемым некоторыми необязательными колонками.';



-- Запросы размещения падают сюда.
CREATE TABLE sio2.adv2_req
(
      CONSTRAINT adv2_req_pkey PRIMARY KEY (id),
      CONSTRAINT adv2_req_prod_contract_id_fkey FOREIGN KEY (prod_contract_id)
          REFERENCES sio2.bill_contract (id) MATCH SIMPLE
          ON UPDATE RESTRICT ON DELETE RESTRICT,
	  CONSTRAINT adv2_req_mode_check CHECK (mode = 'r'::"char")
)
INHERITS (sio2.adv2)
WITH (
    OIDS=FALSE
);
ALTER TABLE sio2.adv2_req
OWNER TO sio2;
COMMENT ON TABLE sio2.adv2_req
IS 'Таблица с очередью размещений на модерацию.
Тут только неподтвержденные размещения, их обычно мало.';



-- approved, подтвержденные (прошедшие модерацию) размещения.
CREATE TABLE sio2.adv2_approved
(
      CONSTRAINT adv2_approved_pkey PRIMARY KEY (id),
      CONSTRAINT adv2_approved_mode_check CHECK (mode = 'a'::"char")
)
INHERITS (sio2.adv2)
WITH (
      OIDS=FALSE
);
ALTER TABLE sio2.adv2_approved
  OWNER TO sio2;



-- online -- текущие активные размещения.
CREATE TABLE sio2.adv2_online
(
      CONSTRAINT adv2_online_pkey PRIMARY KEY (id),
      CONSTRAINT adv2_online_mode_check CHECK (mode = 'o'::"char")
)
INHERITS (sio2.adv2)
WITH (
      OIDS=FALSE
);
ALTER TABLE sio2.adv2_online
  OWNER TO sio2;



-- Размещение уже завершено. Тут по сути историческая свалка размещений.
CREATE TABLE sio2.adv2_done
(
      CONSTRAINT adv2_done_pkey PRIMARY KEY (id),
      CONSTRAINT adv2_done_mode_check CHECK (mode = 'e'::"char" OR mode = 'f'::"char")
)
INHERITS (sio2.adv2)
WITH (
      OIDS=FALSE
);
ALTER TABLE sio2.adv2_done
  OWNER TO sio2;

CREATE INDEX adv2_done_ad_id_idx
  ON sio2.adv2_done
  USING btree
  (ad_id COLLATE pg_catalog."default");





