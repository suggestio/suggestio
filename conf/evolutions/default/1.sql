-- Создать схему
CREATE SCHEMA sio2 AUTHORIZATION sio2;

COMMENT ON SCHEMA sio2
  IS 'Схемота для suggest.io v2.';


-- В этом файле происходит базовое создание начальной схемы, связанной с магазинами.
CREATE TABLE sio2.le_ids (
  id serial NOT NULL
);
ALTER TABLE sio2.le_ids OWNER TO sio2;
COMMENT ON TABLE sio2.le_ids
  IS 'Глобальные id''шники для юрлиц/юрединиц: компаний, ТЦшек, магазинов (le = legal entity).';



CREATE TABLE sio2.company (
  -- Унаследована from table sio2.le_ids:  id integer NOT NULL DEFAULT nextval('sio2.le_ids_id_seq'::regclass),
  name character varying(64), -- Название конторы.
  date_created timestamp(0) with time zone NOT NULL DEFAULT now(), -- Дата добавления этого ряда в таблицу.
  CONSTRAINT company_pkey PRIMARY KEY (id)
)
INHERITS (sio2.le_ids);
ALTER TABLE sio2.company OWNER TO sio2;
COMMENT ON TABLE sio2.company
  IS 'Список контор, которые зареганы в системе. Конторы могут владель магазинами и торговыми центрами, которые содержат эти или другие магазины.';
COMMENT ON COLUMN sio2.company.name IS 'Название конторы.';
COMMENT ON COLUMN sio2.company.date_created IS 'Дата добавления этого ряда в таблицу.';



CREATE TABLE sio2.mart (
  -- Унаследована from table sio2.ids:  id integer NOT NULL DEFAULT nextval('sio2.ids_id_seq'::regclass),
  company_id integer NOT NULL, -- Компания-владелец
  name character varying(64) NOT NULL, -- Название ТЦ.
  address character varying(128) NOT NULL,
  site_url character varying(128), -- Ссылка на сайт магазина, если есть.
  date_created timestamp(0) with time zone NOT NULL DEFAULT now(),
  CONSTRAINT mart_pkey PRIMARY KEY (id),
  CONSTRAINT mart_company_id_fkey FOREIGN KEY (company_id)
      REFERENCES sio2.company (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
)
INHERITS (sio2.le_ids);
ALTER TABLE sio2.mart OWNER TO sio2;
COMMENT ON TABLE sio2.mart
  IS 'Торговые центры и помещения, предназначенные для размещения торговых площадок (магазинов)';
COMMENT ON COLUMN sio2.mart.company_id IS 'Компания-владелец';
COMMENT ON COLUMN sio2.mart.name IS 'Название ТЦ.';
COMMENT ON COLUMN sio2.mart.site_url IS 'Ссылка на сайт магазина, если есть.';



CREATE TABLE sio2.shop (
  -- Унаследована from table sio2.le_ids:  id integer NOT NULL DEFAULT nextval('sio2.le_ids_id_seq'::regclass),
  company_id integer NOT NULL,
  mart_id integer NOT NULL,
  name character varying(64) NOT NULL, -- Отображаемое название магазина.
  date_created timestamp(0) with time zone NOT NULL DEFAULT now(),
  description character varying(2048),   --ALTER TABLE sio2.shop ADD COLUMN description character varying(2048);
  mart_floor integer,
  mart_section integer,
  CONSTRAINT shop_pkey PRIMARY KEY (id),
  CONSTRAINT shop_company_id_fkey FOREIGN KEY (company_id)
      REFERENCES sio2.company (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT shop_mart_id_fkey FOREIGN KEY (mart_id)
      REFERENCES sio2.mart (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
INHERITS (sio2.le_ids);
ALTER TABLE sio2.shop OWNER TO sio2;
COMMENT ON TABLE sio2.shop
  IS 'Список магазинов, которые находятся в торговых центрах или соотв.площадях.
Если магазин находится в своём помещении, то inmart_addr не надо указывать.';
COMMENT ON COLUMN sio2.shop.name IS 'Отображаемое название магазина.';
COMMENT ON COLUMN sio2.shop.inmart_addr IS 'Адрес внутри торгового помещения. Внутри ТЦ указывается этаж и номер помещения, а в собственных помещениях NULL.';
COMMENT ON COLUMN sio2.shop.mart_section IS 'Номер секции/павильона в ТЦ.';


-- Список ссылок на прайсы.
CREATE TABLE sio2.shop_pricelist
(
  id serial NOT NULL,
  shop_id integer NOT NULL,
  url character varying(255) NOT NULL, -- Ссылка на прайс-лист магазина.
  auth_info character varying(128), -- Имя:пароль либо NULL.
  CONSTRAINT shop_pricelist_pkey PRIMARY KEY (id),
  CONSTRAINT shop_pricelist_shop_id_fkey FOREIGN KEY (shop_id)
      REFERENCES sio2.shop (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);
ALTER TABLE sio2.shop_pricelist
  OWNER TO sio2;
COMMENT ON TABLE sio2.shop_pricelist
  IS 'Прайс-листы магазинов.';
COMMENT ON COLUMN sio2.shop_pricelist.url IS 'Ссылка на прайс-лист магазина.';
COMMENT ON COLUMN sio2.shop_pricelist.auth_info IS 'Имя:пароль либо NULL.';

