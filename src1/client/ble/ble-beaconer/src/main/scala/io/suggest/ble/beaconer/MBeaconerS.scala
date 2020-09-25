package io.suggest.ble.beaconer

import diode.data.Pot
import diode.{Effect, FastEq}
import io.suggest.ble.BeaconsNearby_t
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.common.empty.EmptyProduct
import io.suggest.common.html.HtmlConstants
import io.suggest.sjs.common.model.MTsTimerId
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:01
  * Description: Модель состояния для Beaconer-контроллера.
  * Является неявно-пустой, т.е. живёт без Option.
  */
object MBeaconerS {

  def empty = MBeaconerS()

  @inline implicit def univEq: UnivEq[MBeaconerS] = UnivEq.force

  implicit object MBeaconerSFastEq extends FastEq[MBeaconerS] {
    override def eqv(a: MBeaconerS, b: MBeaconerS): Boolean = {
      (a.isEnabled ===* b.isEnabled) &&
        (a.afterOnOff eq b.afterOnOff) &&
        (a.notifyAllTimer ===* b.notifyAllTimer) &&
        (a.beacons ===* b.beacons) &&
        (a.nearbyReport ===* b.nearbyReport) &&
        (a.gcIntervalId ===* b.gcIntervalId) &&
        (a.envFingerPrint ===* b.envFingerPrint) &&
        (a.bleBeaconsApi ===* b.bleBeaconsApi) &&
        (a.opts ==* b.opts)
    }
  }

  def isEnabled = GenLens[MBeaconerS](_.isEnabled)
  def afterOnOff = GenLens[MBeaconerS]( _.afterOnOff )
  def notifyAllTimer = GenLens[MBeaconerS](_.notifyAllTimer)
  lazy val beacons = GenLens[MBeaconerS](_.beacons)
  def gcIntervalId = GenLens[MBeaconerS](_.gcIntervalId)
  val nearbyReport = GenLens[MBeaconerS](_.nearbyReport)
  def opts = GenLens[MBeaconerS]( _.opts )

}


/** Контейнер данных состояния мониторилки маячков.
  *
  * @param isEnabled Изначально - флаг, но он имеет более широкое значение.
  *                  Pot.empty - сейчас BLE API недоступно на устройстве.
  *                  true - включено, true+pending - ВКЛючается
  *                  false - выключено, false+pending - ВЫКЛючается
  * @param afterOnOff После включения/выключения надо исполнить указанный эффект.
  * @param beacons Текущая карта маячков по id.
  * @param envFingerPrint Хэш некоторых данных из карты маячков: чек-сумма текущего состояния.
  * @param bleBeaconsApi Текущее активное API.
  *                      empty - нет подключённого API на текущий момент.
  *                      Ready(timerId) - Нормальный режим работы: об изменениях система уведомляется
  *                      Pending - идёт активация API в фоне.
  *                      Error - не удалось задействовать обнаруженное API.
  * @param gcIntervalId id таймера интервала сборки мусора.
  * @param nearbyReport Итоговый отчёт по текущим наблюдаемым маячкам.
  *                     Подписка на его изменения позволяет определять появление/исчезновение маячков.
  */
case class MBeaconerS(
                       isEnabled            : Pot[Boolean]               = Pot.empty,
                       afterOnOff           : Option[Effect]             = None,
                       notifyAllTimer       : Option[MTsTimerId]         = None,
                       beacons              : Map[String, MBeaconData]   = Map.empty,
                       nearbyReport         : BeaconsNearby_t            = Nil,
                       gcIntervalId         : Option[Int]                = None,
                       envFingerPrint       : Option[Int]                = None,
                       bleBeaconsApi        : Pot[IBleBeaconsApi]        = Pot.empty,
                       opts                 : MBeaconerOpts              = MBeaconerOpts.default,
                     )
  extends EmptyProduct
{

  /** Отчёт по id маяков. */
  lazy val nearbyReportById = nearbyReport.zipWithIdIter.toMap

  // Перезапись toString, чтобы лишний мусор не рендерить.
  override final def toString: String = {
    import HtmlConstants._

    new StringBuilder(128, productPrefix)
      .append( `(` )
      .append( isEnabled ).append( COMMA )
      .append( notifyAllTimer )
      // Вместо карты маячков генерим упрощённый список из укороченных id маячков:
      .append( beacons.size ).append( COLON )
      .append( `[` )
      .appendAll(
        beacons
          .keysIterator
          .flatMap { bcnId =>
            // Генерим строку вида "02...43A5"
            bcnId.substring(0, 2) ::
              HtmlConstants.ELLIPSIS ::
              bcnId.substring(bcnId.length - 4, bcnId.length) ::
              COMMA ::
              Nil
          }
          .flatMap(_.toCharArray)
      )
      .append( `]` ).append( COMMA )
      // mearbyReport: укорачиваем просто до длины
      .append( nearbyReport.length ).append( DIEZ ).append( COMMA )
      .append( gcIntervalId ).append( COMMA )
      .append( envFingerPrint ).append( COMMA )
      .append( bleBeaconsApi ).append( COMMA )
      .append( opts )
      .append( `)` )
      .toString()
  }

}

