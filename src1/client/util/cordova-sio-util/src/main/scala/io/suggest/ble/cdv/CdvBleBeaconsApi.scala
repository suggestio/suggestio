package io.suggest.ble.cdv

import cordova.plugins.ble.central.{Ble, BtDevice, StartScanOptions}
import io.suggest.ble.BleConstants
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.radio.beacon.IBeaconsListenerApi
import io.suggest.radio.{MRadioSignalJs, MRadioSignalTypes}
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 17:28
  * Description: Stateless [[IBeaconsListenerApi]] implementation for beacon scanning via cordova-ble-central plugin.
  */
final class CdvBleBeaconsApi
  extends IBeaconsListenerApi
  with Log
{

  override def radioSignalType = MRadioSignalTypes.BluetoothEddyStone

  /** Helper method to return async boolean callbacks.
    *
    * @param doIt Side-effecting function takes positive and negative callbacks as arguments.
    * @return Boolean Future.
    *         Event on error, Future return false.
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


  /** Check, is bluetooth is enabled at the moment. */
  override def isPeripheralEnabled(): Future[Boolean] = {
    _syncBoolApiMethodHelper {
      (trueF, falseF) =>
        Ble.isEnabled(
          enabled     = trueF,
          notEnabled  = falseF
        )
    }
  }

  /** Try to enable bluetooth. */
  override def enablePeripheral(): Future[Boolean] = {
    _syncBoolApiMethodHelper {
      (trueF, falseF) =>
        Ble.enable(
          success = trueF,
          refused = falseF
        )
    }
  }


  /** Start listening for BLE-beacons, returning results via function. */
  override def listenBeacons(opts: IBeaconsListenerApi.ListenOptions): Future[_] = {
    Future {
      Ble.startScanWithOptions(
        // services: "short UUID" is enought here. 0xFEAA is mandatory for background EddyStone scanning in iOS and Android.
        services      = js.Array( BleConstants.Beacon.EddyStone.SERVICE_UUID_16B_LC ),
        options = new StartScanOptions {
          override val reportDuplicates = true
          override val scanMode = {
            val Outer = IBeaconsListenerApi.ScanMode
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
        success       = _handleDeviceFound( _: BtDevice, opts ),
        failure       = _handleError(_: js.Any)
      )
    }
  }

  /** Supported beacon parsers. */
  def _BEACON_PARSERS: Seq[BeaconParserFactory] = {
    CdvEddyStoneParser ::
    Nil
  }

  /** Detected an BLE advertisement data from 0xFEAA device.
    * Next, it need to be parsed into beacon data or filtered out.
    */
  def _handleDeviceFound(dev: BtDevice, opts: IBeaconsListenerApi.ListenOptions): Unit = {
    for {
      parserFactory <- _BEACON_PARSERS.iterator
      parseRes <- parserFactory(dev).tryParse()
      radioSignal <- parseRes.toOption
    } {
      // Ok, parsed. Call side-effecting listener with result...
      val signalJs = MRadioSignalJs( radioSignal )
      doDispatch( signalJs :: Nil, opts )
    }
  }

  /** Error occured during scan (de)activation. */
  def _handleError(error: js.Any): Unit = {
    logger.error(ErrorMsgs.BLE_SCAN_ERROR, msg = error)
  }


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

  override def isScannerRestartNeededSettingsOnly(v0: IBeaconsListenerApi.ListenOptions,
                                                  v2: IBeaconsListenerApi.ListenOptions,
                                                  osFamily: Option[MOsFamily]): Boolean = {
    // Android: Restart scan, scanMode changed.
    // iOS ignores these thin scanning settings.
    osFamily.fold(true)(_ ==* MOsFamilies.Android) &&
    (v0.scanMode !=* v2.scanMode)
  }

}
