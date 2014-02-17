-- Запрещаем удалять ТЦ, который содержит хотя бы один магазин в себе.
-- Это упростит ряд вещей в кравлере, индексах, и защитит от ряда ошибок.
ALTER TABLE sio2.shop DROP CONSTRAINT shop_mart_id_fkey;

ALTER TABLE sio2.shop
  ADD FOREIGN KEY (mart_id) REFERENCES sio2.mart (id)
   ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX fki_shop_mart_id_fkey
  ON sio2.shop(mart_id);

