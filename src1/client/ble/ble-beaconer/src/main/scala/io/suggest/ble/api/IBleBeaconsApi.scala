package io.suggest.ble.api

import io.suggest.ble.BeaconDetected
import io.suggest.ble.api.cordova.ble.CordovaBleApi
import io.suggest.common.empty.OptionUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import japgolly.univeq.UnivEq

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:21
  * Description: Интерфейс для конкретных API'шек работы с маячками.
  * Изначально была реализация для cordova-ble, но в планах была ещё реализовать web-bluetooth.
  */

trait IBleBeaconsApi {

  /** Узнать, включён ли bluetooth в данный момент? */
  def isBleEnabled(): Future[Boolean]

  /** Попробовать включить bluetooth. */
  def enableBle(): Future[Boolean]

  /** Доступно ли текущее API для использования? */
  def isApiAvailable: Boolean

  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  def listenBeacons(listener: BeaconDetected => _): Future[_]

  /** Прекратить любое слушанье маячков. */
  def unListenAllBeacons(): Future[_]

  override def toString = getClass.getSimpleName

}


object IBleBeaconsApi extends Log {

  /** Найти доступные API для маячков. */
  def detectApis(): Seq[IBleBeaconsApi] = {
    (for {
      cordovaBleApi <- Try( new CordovaBleApi )
      apiAvail <- {
        val availTry = Try( cordovaBleApi.isApiAvailable )
        for (ex <- availTry.failed)
          logger.log( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED + " " + cordovaBleApi, ex )
        availTry
      }
    } yield {
      OptionUtil
        .maybe(apiAvail)( cordovaBleApi )
        .toList
    })
      .getOrElse( Nil )
  }

  @inline implicit def univEq: UnivEq[IBleBeaconsApi] = UnivEq.force

}
