package io.suggest.dev

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

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
      (a.isReadyPot ===* b.isReadyPot) &&
      (a.isCordova ==* b.isCordova) &&
      (a.osFamily ===* b.osFamily)
    }
  }

  @inline implicit def univEq: UnivEq[MPlatformS] = UnivEq.derive

  def isUsingNow  = GenLens[MPlatformS](_.isUsingNow)
  def isReadyPot  = GenLens[MPlatformS](_.isReadyPot)
  def isCordova   = GenLens[MPlatformS](_.isCordova)
  def osFamily    = GenLens[MPlatformS](_.osFamily)


  implicit final class PlatformOpsExt( private val plat: MPlatformS ) extends AnyVal {

    def isBrowser: Boolean = !plat.isCordova

    def isDlAppAvail: Boolean = isBrowser

    /** Использовать ли специальные стили/компоненты под Apple iOS? */
    def isUseIosStyles: Boolean =
      plat.osFamily contains[MOsFamily] MOsFamilies.Apple_iOS

    def isReady: Boolean =
      plat.isReadyPot contains true

  }

  def lastModifiedMs() = System.currentTimeMillis()

}


/** Контейнер данных платформы.
  *
  * @param isReadyPot Готовность платформы к полноценной работе.
  *                   pending означает переключение в противоположное состояние.
  * @param isCordova Является ли текущеая платформа cordova-окружением?
  * @param isUsingNow Активная работа с приложением сейчас?
  *                   true значит, что вкладка открыта и активна, приложение открыто и на переднем плане.
  *                   Все системы должны быть активны.
  *                   false значит, что выдача неактивна: скрыта вкладка, приложение где-то в фоне. Спать.
  * @param osFamily Семейство ОС. Может быть и None, если не удалось сопоставить по модели.
  */
case class MPlatformS(
                       isUsingNow     : Boolean,
                       isReadyPot     : Pot[Boolean] = Pot.empty,
                       isCordova      : Boolean,
                       osFamily       : Option[MOsFamily],
                     ) {

  /** Сохраняем время последней модификации (suspend/resume), чтобы можно было расчитать,
    * сколько времени приложение спало или бодрствовало.
    */
  val lastModifiedMs = MPlatformS.lastModifiedMs()

}
