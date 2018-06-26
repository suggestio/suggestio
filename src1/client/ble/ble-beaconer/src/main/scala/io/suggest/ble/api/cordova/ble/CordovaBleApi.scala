package io.suggest.ble.api.cordova.ble

import com.github.don.cordova.plugin.ble.central.{Ble, BtDevice, StartScanOptions}
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

  /** Вспомогательная функция для сборки методов, которые дёргают true/false-callback'и.
    *
    * @param onErrorMsg При ошибках - логгировать это сообщения.
    * @param doIt Функция для вызова метода с двумя callback'ами.
    * @return Фьчерс с true/false.
    *         Ошибок метод не возвращает, а тоже false.
    */
  private def _syncBoolApiMethodHelper(doIt: (js.Function0[_], js.Function0[_]) => Unit): Future[Boolean] = {
    Future {
      val p = Promise[Boolean]()
      def setIsEnabled(isEnabled: Boolean) = {
        () => p.success(isEnabled)
      }
      doIt(
        setIsEnabled(true),
        setIsEnabled(false)
      )
      p.future
    }
      .flatten
  }


  override def isApiAvailable: Boolean = {
    !js.isUndefined(Ble)
  }


  /** Узнать, включён ли bluetooth в данный момент? */
  override def isBleEnabled(): Future[Boolean] = {
    _syncBoolApiMethodHelper {
      (trueF, falseF) =>
        Ble.isEnabled(
          enabled     = trueF,
          notEnabled  = falseF
        )
    }
  }

  /** Попробовать включить bluetooth. */
  override def enableBle(): Future[Boolean] = {
    _syncBoolApiMethodHelper {
      (trueF, falseF) =>
        Ble.enable(
          success = trueF,
          refused = falseF
        )
    }
  }


  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  override def listenBeacons(listener: Function1[BeaconDetected, _]): Future[_] = {
    Future {
      Ble.startScanWithOptions(
        // Короткого UUID тут достаточно.
        services      = js.Array( /*BleConstants.Beacon.EddyStone.SERVICE_UUID_16B_LC*/ ),
        options = new StartScanOptions {
          override val reportDuplicates = true
        },
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
    f( EddyStoneParser )
      // Среагировать на результат работы цепочки парсеров.
      .fold [Unit] {
        // device found, но почему-то неподходящий под маячок. warn для отправки на сервер сообщения о подозрительной штуковине, потом надо закомментить/упростить.
        LOG.log( WarnMsgs.UNKNOWN_BLE_DEVICE, msg = JSON.stringify(dev) )
      } {
        case Right(beacon) =>
          val e = BeaconDetected( beacon )
          listener(e)
        case _ =>
          // Left(_) - значит парсер отсеял устройство за ненадобностью. do nothing
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
