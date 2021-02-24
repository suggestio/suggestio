package io.suggest.ble.api.cordova.ble

import com.github.don.cordova.plugin.ble.central.{Ble, BtDevice, StartScanOptions}
import io.suggest.ble.{BeaconDetected, BleConstants}
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

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
    * @param doIt Функция для вызова метода с двумя callback'ами.
    * @return Фьчерс с true/false.
    *         Ошибок метод не возвращает, а тоже false.
    */
  private def _syncBoolApiMethodHelper(doIt: (js.Function0[Unit], js.Function0[Unit]) => Unit): Future[Boolean] = {
    Future {
      val p = Promise[Boolean]()
      def setIsEnabled(isEnabled: Boolean): () => Unit = {
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


  override def isApiAvailable: Boolean =
    JsApiUtil.isDefinedSafe( Ble )


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
  override def listenBeacons(opts: IBleBeaconsApi.ListenOptions): Future[_] = {
    Future {
      Ble.startScanWithOptions(
        // Короткого UUID тут достаточно. 0xFEAA обязателен здесь для ФОНОВОГО сканирования на iOS и Android.
        services      = js.Array( BleConstants.Beacon.EddyStone.SERVICE_UUID_16B_LC ),
        options = new StartScanOptions {
          override val reportDuplicates = true
          // TODO Выставлять LOW_LATENCY при открытом сканере узлов.
          // TODO Продумать вариант использования LOW_ENERGY scan.
          override val scanMode = {
            val Outer = IBleBeaconsApi.ScanMode
            val CdvBle = StartScanOptions.ScanMode
            if (opts.scanMode ==* Outer.BALANCED) {
              CdvBle.BALANCED
            } else if (opts.scanMode ==* Outer.FULL_POWER) {
              CdvBle.LOW_LATENCY
            } else if (opts.scanMode ==* Outer.LOW_POWER) {
              CdvBle.LOW_POWER
            } else if (opts.scanMode ==* Outer.OPPORTUNISTIC) {
              CdvBle.OPPORTUNISTIC
            } else {
              throw new IllegalArgumentException( opts.toString + " " + opts.scanMode )
            }
          }
        },
        success       = _handleDeviceFound(_: BtDevice, opts.onBeacon),
        failure       = _handleError(_: js.Any)
      )
    }
  }


  /**
    * Получена инфа с каким-то bluetooth advertisement, необязательно по маячкам.
    * Надо бы распарсить это в поддержимаемые маячки или отфильтровать.
    */
  def _handleDeviceFound(dev: BtDevice, listener: Function1[BeaconDetected, Unit]): Unit = {
    // Заинлайненный список beacon-парсеров.
    // Функция собирает один beacon-парсер и пытается провести парсинг...
    val f = { parserFactory: BeaconParserFactory =>
      parserFactory(dev).tryParse()
    }

    // Заинлайнен список поддерживаемых beacon-парсеров с помощью f(...) orElse f(...) orElse f(...)
    f( EddyStoneParser )
      // Среагировать на результат работы цепочки парсеров.
      .foreach {
        case Right(beacon) =>
          val e = BeaconDetected( beacon )
          listener(e)
        case _ =>
          // Left(_) - значит парсер отсеял устройство за ненадобностью. do nothing
      }
  }

  /** Какая-то ошибка возникла при сканировании. */
  def _handleError(error: js.Any): Unit = {
    logger.error(ErrorMsgs.BLE_SCAN_ERROR, msg = error)
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

  override def isScannerRestartNeededSettingsOnly(v0: IBleBeaconsApi.ListenOptions,
                                                  v2: IBleBeaconsApi.ListenOptions,
                                                  osFamily: Option[MOsFamily]): Boolean = {
    // Android: перезапускать сканирование при изменении scanMode.
    // iOS игнорит тонкие настройки сканирования.
    osFamily.fold(true)(_ ==* MOsFamilies.Android) &&
    (v0.scanMode !=* v2.scanMode)
  }

}
