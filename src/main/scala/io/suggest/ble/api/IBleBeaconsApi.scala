package io.suggest.ble.api

import io.suggest.ble.api.cordova.ble.CordovaBleApi
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.util.SjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:21
  * Description: Интерфейс для конкретных API'шек работы с маячками.
  * Изначально была реализация для cordova-ble, но в планах была ещё реализовать web-bluetooth.
  */

trait IBleBeaconsApi {

  /** Доступно ли текущее API для использования? */
  def isApiAvailable: Boolean

  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  def listenBeacons(listener: SjsFsm): Unit

  /** Прекратить любое слушанье маячков. */
  def unListenAllBeacons(): Unit

}


object IBleBeaconsApi extends SjsLogger {

  /** Выбрать доступное API маячков. */
  val detectApi: Option[IBleBeaconsApi] = {
    val cordovaBleApi = new CordovaBleApi
    try {
      val apiAvail = cordovaBleApi.isApiAvailable
      if (apiAvail) {
        Some(cordovaBleApi)
      } else {
        None
      }
    } catch {
      case ex: Throwable =>
        log( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED + " " + cordovaBleApi, ex )
        None
    }

  }

}
