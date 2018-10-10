BEGIN;

-- balance
UPDATE sio2.balance SET amount = amount * 100, blocked = blocked * 100, low = low * 100;

ALTER TABLE sio2.balance
    ALTER COLUMN amount TYPE bigint;

ALTER TABLE sio2.balance
    ALTER COLUMN blocked TYPE bigint;

ALTER TABLE sio2.balance
    ALTER COLUMN low TYPE bigint;

ALTER TABLE sio2.balance DROP CONSTRAINT balance_blocked_check;
ALTER TABLE sio2.balance
    ADD CONSTRAINT balance_blocked_check CHECK (blocked >= 0);

ALTER TABLE sio2.balance DROP CONSTRAINT balance_check;
ALTER TABLE sio2.balance
    ADD CONSTRAINT balance_check CHECK (amount >= COALESCE(low, 0));


-- debug
TRUNCATE sio2.debug;


-- item
UPDATE sio2.item SET amount = amount * 100;

ALTER TABLE sio2.item
  ALTER COLUMN amount TYPE bigint;

ALTER TABLE sio2.item DROP CONSTRAINT item_amount_check;
ALTER TABLE sio2.item
    ADD CONSTRAINT item_amount_check CHECK (amount >= 0);

ALTER TABLE sio2.item
    RENAME ad_id TO node_id;

COMMENT ON COLUMN sio2.item.node_id
    IS 'id узла, связанного с этим item''ом';


-- txn
UPDATE sio2.txn SET amount = amount * 100;

ALTER TABLE sio2.txn
  ALTER COLUMN amount TYPE bigint;


COMMIT;

