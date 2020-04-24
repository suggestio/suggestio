package io.suggest.ble.beaconer

import japgolly.univeq.UnivEq
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

  def hard = GenLens[MBeaconerOpts]( _.hardOff )
  def askEnableBt = GenLens[MBeaconerOpts]( _.askEnableBt )
  def oneShot = GenLens[MBeaconerOpts]( _.oneShot )
  //def nearbyChanged = GenLens[MBeaconerOpts]( _.nearbyChanged )

}


/** Контейнер настроек запуска/остановки bluetooth.
  *
  * @param hardOff Жесткая фиксация выключения или жесткое включение.
  *                hard=true + off => жесткое выключение, для переключения которого требуется hard=true + on.
  * @param askEnableBt Разрешить запрашивать включение bluetooth у юзера.
  *                    true - Если bluetooth выключен в ОС, будет открыт системный диалог.
  *                    false - Если bt выключен на уровне ОС, то beaconer будет деактивирован следом.
  * @param oneShot Одноразовое сканирование радиоэфира, и выключение после окончания сканирования.
  * @param nearbyChanged Функция, возвращающая эффект реакции на изменение списка близлежащих маячков.
  *                      $1 - None - ничего не изменилось. Some() - список действительно изменился.
  */
case class MBeaconerOpts(
                          hardOff           : Boolean           = false,
                          askEnableBt       : Boolean           = true,
                          oneShot           : Boolean           = false,
                          //nearbyChanged     : Option[Option[Seq[MUidBeacon]] => Effect] = None,
                        )
