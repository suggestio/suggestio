package io.suggest.ble.beaconer.m

import diode.FastEq
import diode.data.Pot
import io.suggest.ble.MUidBeacon
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.common.empty.EmptyProduct
import io.suggest.sjs.common.model.MTsTimerId
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:01
  * Description: Модель состояния для Beaconer-контроллера.
  * Является неявно-пустой, т.е. живёт без Option.
  */
object MBeaconerS {

  def empty = MBeaconerS()

  implicit def univEq: UnivEq[MBeaconerS] = UnivEq.force

  implicit object MBeaconerSFastEq extends FastEq[MBeaconerS] {
    override def eqv(a: MBeaconerS, b: MBeaconerS): Boolean = {
      (a.isEnabled ===* b.isEnabled) &&
        (a.notifyAllTimer ===* b.notifyAllTimer) &&
        (a.beacons ===* b.beacons) &&
        (a.nearbyReport ===* b.nearbyReport) &&
        (a.gcIntervalId ===* b.gcIntervalId) &&
        (a.envFingerPrint ===* b.envFingerPrint) &&
        (a.bleBeaconsApi ===* b.bleBeaconsApi) &&
        (a.hardOff ==* b.hardOff)
    }
  }

}


/** Контейнер данных состояния мониторилки маячков.
  *
  * @param isEnabled Изначально - флаг, но он имеет более широкое значение.
  *                  Pot.empty - сейчас BLE API недоступно на устройстве.
  *                  true - включено, true+pending - ВКЛючается
  *                  false - выключено, false+pending - ВЫКЛючается
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
  * @param hardOff      Жесткое выключение системы. Флаг для защиты от автоматических включений-выключений,
  *                     когда пользователь требует выключить bluetooth.
  */
case class MBeaconerS(
                       isEnabled            : Pot[Boolean]               = Pot.empty,
                       notifyAllTimer       : Option[MTsTimerId]         = None,
                       beacons              : Map[String, MBeaconData]   = Map.empty,
                       nearbyReport         : Seq[MUidBeacon]            = Nil,
                       gcIntervalId         : Option[Int]                = None,
                       envFingerPrint       : Option[Int]                = None,
                       bleBeaconsApi        : Pot[IBleBeaconsApi]        = Pot.empty,
                       hardOff              : Boolean                    = false,
                     )
  extends EmptyProduct
{

  // Изоляция толстых вызовов copy здесь для снижения объемов кодогенерации:

  def withIsEnabled(isEnabled: Pot[Boolean]) =
    copy( isEnabled = isEnabled )
  def withBeacons(beacons2: Map[String, MBeaconData]) =
    copy( beacons = beacons2 )
  def withEnvFingerPrint(envFingerPrint: Option[Int]) =
    copy( envFingerPrint = envFingerPrint )
  def withNotifyAllTimerId(notifyAllTimer: Option[MTsTimerId]) =
    copy(notifyAllTimer = notifyAllTimer)
  def withBleBeaconsApi(bleBeaconsApi: Pot[IBleBeaconsApi]) =
    copy( bleBeaconsApi = bleBeaconsApi )
  def withGcIntervalId( gcIntervalId: Option[Int] ) =
    copy( gcIntervalId = gcIntervalId )
  def withHardOff(hardOff: Boolean) =
    copy(hardOff = hardOff)

}

