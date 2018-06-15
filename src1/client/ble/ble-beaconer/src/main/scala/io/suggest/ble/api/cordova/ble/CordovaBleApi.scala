package io.suggest.ble.api.cordova.ble

import com.github.don.cordova.plugin.ble.central.{Ble, BtDevice}
import io.suggest.ble.BleConstants
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m.BeaconDetected
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 17:28
  * Description: Поддержка сканирования маячков через cordova-plugin-ble от evothings.
  *
  * @see [[https://github.com/evothings/cordova-ble]]
  */

class CordovaBleApi extends IBleBeaconsApi with Log {

  override def isApiAvailable: Boolean = {
    !js.isUndefined(Ble)
  }

  override def toString: String = {
    getClass.getSimpleName
  }


  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  override def listenBeacons(listener: Function1[BeaconDetected, _]): Future[_] = {
    Future {
      Ble.startScan(
        // Короткого UUID тут достаточно.
        services      = js.Array( BleConstants.Beacon.EddyStone.SERVICE_UUID_16B_LC ),
        success       = _handleDeviceFound(_: BtDevice, listener),
        failure       = _handleError(_: js.Any)
      )
    }
  }


  /**
    * Получена инфа с каким-то bluetooth advertisement, необязательно по маячкам.
    * Надо бы распарсить это в поддержимаемые маячки или отфильтровать.
    */
  def _handleDeviceFound(dev: BtDevice, listener: Function1[BeaconDetected, _]): Unit = {
    // Заинлайненный список beacon-парсеров.
    // Функция собирает один beacon-парсер и пытается провести парсинг...
    val f = { parserFactory: BeaconParserFactory =>
      parserFactory(dev).tryParse()
    }

    // Заинлайнен список поддерживаемых beacon-парсеров с помощью f(...) orElse f(...) orElse f(...)
    def devStr = JSON.stringify(dev)
    f( EddyStoneParser )
      // Среагировать на результат работы цепочки парсеров.
      .fold [Unit] {
        // device found, но почему-то неподходящий под маячок. warn для отправки на сервер сообщения о подозрительной штуковине, потом надо закомментить/упростить.
        LOG.log( WarnMsgs.UNKNOWN_BLE_DEVICE, msg = devStr )
      } {
        case Right(beacon) =>
          val e = BeaconDetected( beacon )
          listener(e)
        case _ =>
          // do nothing
        //case Left(msg) =>
          //LOG.log( WarnMsgs.FILTERED_OUT_BLE_DEVICE, msg = devStr + " " + msg )
      }
  }

  /** Какая-то ошибка возникла при сканировании. */
  def _handleError(error: js.Any): Unit = {
    LOG.error(ErrorMsgs.BLE_SCAN_ERROR, msg = error)
  }


  /** Прекратить любое слушанье маячков. */
  override def unListenAllBeacons(): Future[_] = {
    Future {
      val p = Promise[None.type]()
      Ble.stopScan(
        success = ()  => p.success( None ),
        failure = (r) => p.failure( new RuntimeException(r.toString) )
      )
      p.future
    }
      .flatten
  }

}
