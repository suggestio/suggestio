-- sio_comission перенесён из контракта на уровень тарифа.
-- В mmp-тариф добавлен флаг sink.

BEGIN;


CREATE TABLE sio2.sink_comission
(
  id serial NOT NULL,
  sink "char" NOT NULL,
  contract_id integer NOT NULL,
  sio_comission real NOT NULL, -- комиссия s.io с прибыли в этой выдаче
  CONSTRAINT sink_comission_pkey PRIMARY KEY (id),
  CONSTRAINT sink_comission_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT sink_comission_contract_id_sink_key UNIQUE (contract_id, sink)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.sink_comission
  OWNER TO sio2;
COMMENT ON TABLE sio2.sink_comission
  IS 'комиссия s.io в зависимости от используемой выдачи';
COMMENT ON COLUMN sio2.sink_comission.sio_comission IS 'комиссия s.io с прибыли в этой выдаче';


CREATE INDEX fki_sink_comission_contract_id_fkey
  ON sio2.sink_comission
  USING btree
  (contract_id);


-- Заливаем в таблицу данные из bill_contract
INSERT INTO sio2.sink_comission(contract_id, sink, sio_comission)
(SELECT bc.id, 'w', bc.sio_comission FROM sio2.bill_contract bc);

-- выкинуть колонку из старого месторасположения.
ALTER TABLE sio2.bill_contract DROP COLUMN sio_comission;


-- sio2.adv содержит колонку show_levels несовместимого типа. Нужно это исправить.
ALTER TABLE sio2.adv ADD COLUMN show_levels2 character varying[];

UPDATE sio2.adv SET show_levels2 = show_levels;

ALTER TABLE sio2.adv DROP COLUMN show_levels;

ALTER TABLE sio2.adv RENAME show_levels2  TO show_levels;
ALTER TABLE sio2.adv
   ALTER COLUMN show_levels SET DEFAULT '{}'::varchar[];
ALTER TABLE sio2.adv
   ALTER COLUMN show_levels SET NOT NULL;
COMMENT ON COLUMN sio2.adv.show_levels
  IS 'Запрашиваемые уровни отображения.';


COMMIT;

