-- Начат запил в сторону поддержки отчисления роялти другим узлам.

BEGIN;


CREATE TABLE sio2.bill_royalty
(
  id serial NOT NULL,
  contract_id integer NOT NULL,
  royalty real NOT NULL, -- доля отчисляемого роялти
  is_internal boolean NOT NULL, -- true - внутреннее роялти, т.е. отчисление с текущей прибыли узла, не меняя цифры в тарифе....
  to_adn_id character varying(32) NOT NULL, -- id узла, в кошелёк к которому должны падать эти отчисления.
  CONSTRAINT bill_royalty_pkey PRIMARY KEY (id),
  CONSTRAINT bill_royalty_contract_id_fkey FOREIGN KEY (contract_id)
      REFERENCES sio2.bill_contract (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT bill_royalty_royalty_check CHECK (royalty > 0 AND royalty <= 1.0)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE sio2.bill_royalty
  OWNER TO sio2;
COMMENT ON TABLE sio2.bill_royalty
  IS 'Описание гонораров с прибылей для других узлов.';
COMMENT ON COLUMN sio2.bill_royalty.royalty IS 'доля отчисляемого роялти';
COMMENT ON COLUMN sio2.bill_royalty.is_internal IS 'true - внутреннее роялти, т.е. отчисление с текущей прибыли узла, не меняя цифры в тарифе.
false - внешнее роялти, т.е. изменяются тарифные цифры вверх.';
COMMENT ON COLUMN sio2.bill_royalty.to_adn_id IS 'id узла, в кошелёк к которому должны падать эти отчисления.';


COMMIT;

