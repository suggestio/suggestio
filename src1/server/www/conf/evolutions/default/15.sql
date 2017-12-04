BEGIN;

ALTER TABLE sio2.item
    ADD COLUMN tag_node_id character varying(64);

COMMENT ON COLUMN sio2.item.tag_node_id
    IS 'id узла-тега, с которым связано размещение в теге.
Раньше значение id узла-тега хранилось прямо в rcvr_id, что было очень неправильно.';


CREATE INDEX item_tag_node_id_inx
    ON sio2.item USING btree
    (tag_node_id ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX item_rcvr_id_inx
    ON sio2.item USING btree
    (rcvr_id ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX item_node_id_inx
    ON sio2.item USING btree
    (ad_id ASC NULLS LAST)
    TABLESPACE pg_default;


COMMIT;

