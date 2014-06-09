-- В форму размещения рекламной карточки добавлены уровни отображения
-- вместо галочки on_start_page:: boolean.
-- Нужно перепилить adv-таблицы через родительскую таблицу.


BEGIN;

ALTER TABLE sio2.adv
   ADD COLUMN show_levels "char"[] NOT NULL DEFAULT '{}'::"char"[];
COMMENT ON COLUMN sio2.adv.show_levels
  IS 'Запрашиваемые уровни отображения.';

UPDATE sio2.adv SET show_levels = '{d}'::"char"[] WHERE on_start_page;

ALTER TABLE sio2.adv DROP COLUMN on_start_page;


ALTER TABLE sio2.bill_mmp_daily
  ADD COLUMN on_rcvr_cat real NOT NULL DEFAULT 2.0;
COMMENT ON COLUMN sio2.bill_mmp_daily.on_rcvr_cat
  IS 'Относительная наценка за размещения в каталоге узла-получателя.';
ALTER TABLE sio2.bill_mmp_daily
   ALTER COLUMN on_rcvr_cat DROP DEFAULT;


COMMIT;

