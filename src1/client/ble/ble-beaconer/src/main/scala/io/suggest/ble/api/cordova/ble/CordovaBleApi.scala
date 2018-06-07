package io.suggest.ble.api.cordova.ble

import cordova.Cordova
import evothings.ble.{BLE, DeviceInfo}
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m.BeaconDetected
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
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

  /** Динамический (через require()) инстанс cordova-plugin-ble API, если API доступно. */
  val API_OPT = {
    val id = "cordova-plugin-ble.BLE"
    try {
      Option( Cordova.require[BLE](id) )
    } catch {
      case ex: Throwable =>
        LOG.warn(ErrorMsgs.CORDOVA_BLE_REQUIRE_FAILED, msg = id, ex = ex)
        None
    }
  }

  override def isApiAvailable: Boolean = {
    API_OPT.isDefined
  }

  override def toString: String = {
    getClass.getSimpleName + "(" + API_OPT + ")"
  }


  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  override def listenBeacons(listener: Function1[BeaconDetected, _]): Future[_] = {
    Future {
      API_OPT.get.startScan(
        onDeviceFound = _handleDeviceFound(_: DeviceInfo, listener),
        onScanError   = _handleErrorCode(_: String)
      )
    }
  }


  /**
    * Получена инфа с каким-то bluetooth advertisement, необязательно по маячкам.
    * Надо бы распарсить это в поддержимаемые маячки или отфильтровать.
    */
  def _handleDeviceFound(dev: DeviceInfo, listener: Function1[BeaconDetected, _]): Unit = {
    // Заинлайненный список beacon-парсеров.
    // Функция собирает один beacon-парсер и пытается провести парсинг...
    val f = { parserFactory: BeaconParserFactory =>
      parserFactory(dev).tryParse()
    }

    // Заинлайнен список поддерживаемых beacon-парсеров с помощью f(...) orElse f(...) orElse f(...)
    def devStr = JSON.stringify(dev)
    f( EddyStoneParser )
      .orElse( f(IBeaconParser) )
      // Среагировать на результат работы цепочки парсеров.
      .fold [Unit] {
        // device found, но почему-то неподходящий под маячок. warn для отправки на сервер сообщения о подозрительной штуковине, потом надо закомментить/упростить.
        LOG.log( WarnMsgs.UNKNOWN_BLE_DEVICE, msg = devStr )

      } {
        case Right(beacon) =>
          val e = BeaconDetected( beacon )
          listener(e)
        case Left(msg) =>
          LOG.log( WarnMsgs.FILTERED_OUT_BLE_DEVICE, msg = devStr + " " + msg )
      }
  }

  /** Какая-то ошибка возникла при сканировании. */
  def _handleErrorCode(errorCode: String): Unit = {
    LOG.error(ErrorMsgs.BLE_SCAN_ERROR, msg = errorCode)
  }


  /** Прекратить любое слушанье маячков. */
  override def unListenAllBeacons(): Future[_] = {
    Future {
      API_OPT.get.stopScan()
    }
  }

}
