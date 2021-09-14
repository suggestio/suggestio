package io.suggest.wifi

import cordova.plugins.wifi.wizard2.CdvWifiWizard2
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
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


object CdvWifiWizard2BeaconsApi {
  /** @return plugin implementation for device operating system. */
  def forOs(osFamily: MOsFamily): Option[CdvWifiWizard2BeaconsApi] = {
    Option {
      osFamily match {
        case MOsFamilies.Android    => new CdvWifiWizard2BeaconsApi_Android
        case MOsFamilies.Apple_iOS  => new CdvWifiWizard2BeaconsApi_iOS
        case _ => null
      }
    }
  }
}

abstract class CdvWifiWizard2BeaconsApi
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


  /** Is this API implementation available for using? */
  override def isApiAvailable: Boolean = {
    // MOsFamily == Android already checked on upper level.
    JsApiUtil.isDefinedSafe( CdvWifiWizard2 )
  }


  private def _stopCurrentInterval(): Unit = {
    for (ivlId <- _intervalId)
      DomQuick.clearInterval( ivlId )
  }

  def onTimer(opts: IBeaconsListenerApi.ListenOptions): Unit


  def isActiveScan(opts: IBeaconsListenerApi.ListenOptions): Boolean = {
    opts.scanMode ==* IBeaconsListenerApi.ScanMode.FULL_POWER
  }


  /** Start to listen for bluetooth beacon signals. */
  override def listenBeacons(opts: IBeaconsListenerApi.ListenOptions): Future[_] = {
    _stopCurrentInterval()

    val intervalId = DomQuick.setInterval(
      timeMs = (if (isActiveScan(opts)) SCAN_ACTIVE_EVERY_SECONDS else SCAN_PASSIVE_EVERY_SECONDS) * 1000,
    ) { () =>
      onTimer( opts )
    }

    _intervalId = Some(intervalId)

    Future.successful(())
  }


  /** Stop Wi-Fi scanning. */
  override def unListenAllBeacons(): Future[_] = {
    _stopCurrentInterval()
    Future.successful(())
  }

}


/** Stateful API instance for Wi-Fi scanning.
  * Scanning implemented via passive/active wifi-list grabbing.
  */
final class CdvWifiWizard2BeaconsApi_Android
  extends CdvWifiWizard2BeaconsApi
{

  override def canEnable = true

  /** Try to enable Wi-Fi. */
  override def enablePeripheral(): Future[Boolean] = {
    CdvWifiWizard2
      .enableWifi()
      .toFuture
      .transform { case tryRes =>
        Success( tryRes.isSuccess )
      }
  }


  def onTimer(opts: IBeaconsListenerApi.ListenOptions): Unit = {
    val now = Instant.now()

    for {
      scanResults <- (
        if (isActiveScan(opts)) CdvWifiWizard2.scan()
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


  override def isScannerRestartNeededSettingsOnly(v0: IBeaconsListenerApi.ListenOptions,
                                                  v2: IBeaconsListenerApi.ListenOptions,
                                                 ): Boolean = {
    val fullPower = IBeaconsListenerApi.ScanMode.FULL_POWER
    (v0.scanMode ==* fullPower) !=* (v2.scanMode ==* fullPower)
  }

}


final class CdvWifiWizard2BeaconsApi_iOS
  extends CdvWifiWizard2BeaconsApi
{

  override def onTimer(opts: IBeaconsListenerApi.ListenOptions): Unit = {
    for {
      bssidOrNull <- CdvWifiWizard2.getConnectedBSSID().toFuture
      bssidOpt = Option( bssidOrNull )
      if bssidOpt.exists { bssid =>
        val macValid = NetworkingUtil.validateMacAddress( bssid )
        val isOk = macValid.isSuccess
        if (!isOk)
          logger.warn( ErrorMsgs.UNKNOWN_CONNECTION, msg = (bssid, macValid) )
        isOk
      }

      ssidOrNull <- CdvWifiWizard2.getConnectedSSID().toFuture
      ssidOpt = Option( ssidOrNull )
    } {
      val radioSignal = MRadioSignalJs(
        signal = MRadioSignal(
          factoryUid  = bssidOpt
            // Remove delimiters from MAC-address to make it short for URL query-string:
            .map( NetworkingUtil.minifyMacAddress ),
          customName  = ssidOpt,
          rssi        = None,
          typ         = MRadioSignalTypes.WiFi,
        ),
        seenAt = Instant.now(),
      )

      doDispatch( radioSignal :: Nil, opts )
    }
  }


  override def canEnable = false

  override def enablePeripheral(): Future[Boolean] = {
    // TODO If it cannot enabled, return false...
    Future.successful( true )
  }

  override def isScannerRestartNeededSettingsOnly(v0: IBeaconsListenerApi.ListenOptions, v2: IBeaconsListenerApi.ListenOptions): Boolean = {
    false
  }

}
