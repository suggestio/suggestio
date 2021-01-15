package io.suggest.ble.beaconer

import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.2020 10:27
  * Description: Модель настроек beaconer'а.
  */
object MBeaconerOpts {

  def default = apply()

  @inline implicit def univEq: UnivEq[MBeaconerOpts] = UnivEq.force

  def askEnableBt = GenLens[MBeaconerOpts]( _.askEnableBt )
  def oneShot = GenLens[MBeaconerOpts]( _.oneShot )

}


/** Контейнер настроек запуска/остановки bluetooth.
  *
  * @param askEnableBt Разрешить запрашивать включение bluetooth у юзера.
  *                    true - Если bluetooth выключен в ОС, будет открыт системный диалог.
  *                    false - Если bt выключен на уровне ОС, то beaconer будет деактивирован следом.
  * @param oneShot Одноразовое сканирование радиоэфира, и выключение после окончания сканирования.
  */
case class MBeaconerOpts(
                          askEnableBt       : Boolean           = true,
                          oneShot           : Boolean           = false,
                        )
