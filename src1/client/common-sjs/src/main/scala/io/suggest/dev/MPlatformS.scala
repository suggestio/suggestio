package io.suggest.dev

import diode.FastEq
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.18 15:25
  * Description: Модель данных текущей платформы для platform-контроллера.
  */
object MPlatformS {

  implicit object MPlatformSFastEq extends FastEq[MPlatformS] {
    override def eqv(a: MPlatformS, b: MPlatformS): Boolean = {
      (a.isUsingNow ==* b.isUsingNow) &&
      (a.isReady ==* b.isReady) &&
      (a.isCordova ==* b.isCordova) &&
      (a.hasBle ==* b.hasBle) &&
      (a.osFamily ===* b.osFamily)
    }
  }

  @inline implicit def univEq: UnivEq[MPlatformS] = UnivEq.derive

  val isUsingNow  = GenLens[MPlatformS](_.isUsingNow)
  val isReady     = GenLens[MPlatformS](_.isReady)
  def isCordova   = GenLens[MPlatformS](_.isCordova)
  def hasBle      = GenLens[MPlatformS](_.hasBle)
  def osFamily    = GenLens[MPlatformS](_.osFamily)


  implicit final class PlatformOpsExt( private val plat: MPlatformS ) extends AnyVal {

    def isBrowser: Boolean = !plat.isCordova

    def isDlAppAvail: Boolean = isBrowser

    /** Использовать ли специальные стили/компоненты под Apple iOS? */
    def isUseIosStyles: Boolean =
      plat.osFamily contains[MOsFamily] MOsFamilies.Apple_iOS

  }

}


/** Контейнер данных платформы.
  *
  * @param isReady Готовность платформы к полноценной работе.
  * @param isCordova Является ли текущеая платформа cordova-окружением?
  * @param isUsingNow Активная работа с приложением сейчас?
  *                   true значит, что вкладка открыта и активна, приложение открыто и на переднем плане.
  *                   Все системы должны быть активны.
  *                   false значит, что выдача неактивна: скрыта вкладка, приложение где-то в фоне. Спать.
  * @param hasBle Доступен ли Bluetooth LE на данной платформе?
  *               Следует помнить, что BLE может быть доступен, а платформа ещё не готова,
  *               и работа с BLE API будет с не очень предсказуемыми результатами.
  * @param osFamily Семейство ОС. Может быть и None, если не удалось сопоставить по модели.
  */
case class MPlatformS(
                       isUsingNow     : Boolean,
                       isReady        : Boolean,
                       isCordova      : Boolean,
                       hasBle         : Boolean,
                       osFamily       : Option[MOsFamily],
                     )
