package io.suggest.wifi

import cordova.plugins.wifi.wizard2.CdvWifiWizard2
import io.suggest.dev.MOsFamily
import io.suggest.log.Log
import io.suggest.netif.NetworkingUtil
import io.suggest.radio.beacon.IBeaconsListenerApi
import io.suggest.radio.{MRadioSignal, MRadioSignalJs, MRadioSignalTypes}
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._

import java.time.Instant
import scala.concurrent.Future
import scala.util.Success

/** Stateful API instance for Wi-Fi scanning.
  * Scanning implemented via passive/active wifi-list grabbing.
  */
final class CdvWifiWizard2BeaconsApi
  extends IBeaconsListenerApi
  with Log
{

  override def radioSignalType = MRadioSignalTypes.WiFi

  /** Every N seconds passively scan WI-FI BSS.
    * Also check if WI-FI is enabled. */
  private def SCAN_PASSIVE_EVERY_SECONDS = 5

  private def SCAN_ACTIVE_EVERY_SECONDS = 1

  /** State: interval timer id for  */
  private var _intervalId: Option[Int] = None

  /** Is Wi-Fi enabled at the moment? */
  override def isPeripheralEnabled(): Future[Boolean] = {
    //CdvWifiWizard2.isWifiEnabled().toFuture
    // Guessing true, even if wifi is disabled. If user switch wifi on/off during nodes window, scan will work as expected.
    // TODO Suboptimal. Need to shutdown scanning, when wifi disabled.
    Future.successful(true)
  }

  /** Try to enable Wi-Fi. */
  override def enablePeripheral(): Future[Boolean] = {
    CdvWifiWizard2
      .enableWifi()
      .toFuture
      .transform { case tryRes =>
        Success( tryRes.isSuccess )
      }
  }

  /** Is this API implementation available for using? */
  override def isApiAvailable: Boolean = {
    // MOsFamily == Android already checked on upper level.
    JsApiUtil.isDefinedSafe( CdvWifiWizard2 )
  }

  private def _stopCurrentInterval(): Unit = {
    for (ivlId <- _intervalId)
      DomQuick.clearInterval( ivlId )
  }

  /** Start to listen for bluetooth beacon signals. */
  override def listenBeacons(opts: IBeaconsListenerApi.ListenOptions): Future[_] = {
    _stopCurrentInterval()

    val isActiveScan = opts.scanMode ==* IBeaconsListenerApi.ScanMode.FULL_POWER

    val intervalId = DomQuick.setInterval(
      timeMs = (if (isActiveScan) SCAN_ACTIVE_EVERY_SECONDS else SCAN_PASSIVE_EVERY_SECONDS) * 1000,
    ) { () =>
      val now = Instant.now()

      for {
        scanResults <- (
          if (isActiveScan) CdvWifiWizard2.scan()
          else CdvWifiWizard2.getScanResults()
        )
          .toFuture
      } {
        for {
          // Convert timestamp (since device OS boot) to nearest j.t.Instant via comparing with current Instant.
          // This is totally unprecise, but enought for current usage.
          latestTstampD <- scanResults
            .iterator
            .map( _.timestamp )
            .maxOption
          // Guessing: latestTimestamp == now. TODO Increase precision of timestamp convertion.
          latestTstamp = latestTstampD.toLong

          radioSignals = scanResults
            .iterator
            .map { wifi =>
              MRadioSignalJs(
                signal = MRadioSignal(
                  factoryUid  = Option( wifi.BSSID )
                    // Remove delimiters from MAC-address to make it short for URL query-string:
                    .map( NetworkingUtil.minifyMacAddress ),
                  customName  = Option( wifi.SSID ),
                  rssi        = Some( wifi.level ),
                  typ         = MRadioSignalTypes.WiFi,
                ),
                seenAt = {
                  val wifiTstamp = wifi.timestamp.toLong
                  if (wifiTstamp ==* latestTstamp) now
                  else now.minusMillis( latestTstamp - wifiTstamp )
                },
              )
            }
            .to( LazyList )

          if radioSignals.nonEmpty
        } {
          doDispatch( radioSignals, opts )
        }
      }
    }

    _intervalId = Some(intervalId)

    Future.successful(())
  }

  /** Stop Wi-Fi scanning. */
  override def unListenAllBeacons(): Future[_] = {
    _stopCurrentInterval()
    Future.successful(())
  }

  override def isScannerRestartNeededSettingsOnly(v0: IBeaconsListenerApi.ListenOptions,
                                                  v2: IBeaconsListenerApi.ListenOptions,
                                                  osFamily: Option[MOsFamily]): Boolean = {
    val fp = IBeaconsListenerApi.ScanMode.FULL_POWER
    (v0.scanMode ==* fp) !=* (v2.scanMode ==* fp)
  }

}
