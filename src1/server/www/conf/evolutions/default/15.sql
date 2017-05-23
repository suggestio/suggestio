BEGIN;

CREATE TABLE sio2.debug
(
  object_id bigint NOT NULL, -- id элемента биллинга (item'а, ордера, контракта и т.д.)
  key character varying(8) NOT NULL, -- Строковой ключ, описывающий тип дампа.
  vsn smallint NOT NULL, -- Номер версии формата данных.
  data bytea NOT NULL, -- Данные в произвольной форме.
  CONSTRAINT debug_pkey PRIMARY KEY (object_id, key)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.debug
  OWNER TO sio2;
COMMENT ON TABLE sio2.debug
  IS 'Отладочная инфа по элементам биллинга. Появилась для возможности модификации стоимости item''а уже после фактического его обсчёта. В дампе сохранялся сжатый инстанс PriceDsl, пригодный для десериализации, модификации и пересчёта.';
COMMENT ON COLUMN sio2.debug.object_id IS 'id элемента биллинга (item''а, ордера, контракта и т.д.)';
COMMENT ON COLUMN sio2.debug.key IS 'Строковой ключ, описывающий тип дампа.';
COMMENT ON COLUMN sio2.debug.vsn IS 'Номер версии формата данных.';
COMMENT ON COLUMN sio2.debug.data IS 'Данные в произвольной форме.';


-- Index: sio2.debug_object_id_idx

-- DROP INDEX sio2.debug_object_id_idx;

CREATE INDEX debug_object_id_idx
  ON sio2.debug
  USING btree
  (object_id);

COMMIT;
