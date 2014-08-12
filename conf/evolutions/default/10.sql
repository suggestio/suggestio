-- Таблицы для геолокации по ip через дампы от ру-центра (ipgeobase.ru).

BEGIN;


CREATE TABLE sio2.ipgeobase_city
(
  id integer NOT NULL,
  city_name character varying(64) NOT NULL,
  region character varying(128) NOT NULL,
  lat double precision NOT NULL,
  lon double precision NOT NULL,
  CONSTRAINT ipgeobase_city_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.ipgeobase_city
  OWNER TO sio2;




CREATE TABLE sio2.ipgeobase_range
(
  country_iso2 character(2),
  city_id integer,
  start inet NOT NULL,
  "end" inet NOT NULL,
  CONSTRAINT ipgeobase_range_check CHECK (start <= "end")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.ipgeobase_range
  OWNER TO sio2;
COMMENT ON TABLE sio2.ipgeobase_range
  IS 'ipv4-диапазоны адресов и их принадлежность к городам/странам.';


CREATE INDEX ipgeobase_range_end_idx
  ON sio2.ipgeobase_range
  USING btree
  ("end");

CREATE INDEX ipgeobase_range_start_idx
  ON sio2.ipgeobase_range
  USING btree
  (start);


COMMIT;

