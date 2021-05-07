package io.suggest.ble.api

import io.suggest.ble.BeaconDetected
import io.suggest.ble.api.cordova.ble.CordovaBleApi
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MOsFamily
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import japgolly.univeq._

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:21
  * Description: Interface for Bluetooth beacons radio scanning API.
  */

trait IBleBeaconsApi {

  /** Is Bluetooth enabled at the moment? */
  def isBleEnabled(): Future[Boolean]

  /** Try to enable Bluetooth. */
  def enableBle(): Future[Boolean]

  /** Is this API implementation available for using? */
  def isApiAvailable: Boolean

  /** Start to listen for bluetooth beacon signals. */
  def listenBeacons(opts: IBleBeaconsApi.ListenOptions): Future[_]

  /** Stop all bluetooth listening. */
  def unListenAllBeacons(): Future[_]

  /** Is this scanner restart needed, if some settings changed?
    * Do not compared ListenOptions.onBeacon or other function instances. Only compare basic scan options. */
  def isScannerRestartNeededSettingsOnly(
                                          v0: IBleBeaconsApi.ListenOptions,
                                          v2: IBleBeaconsApi.ListenOptions,
                                          osFamily: Option[MOsFamily],
                                        ): Boolean

  /** Is scanner restart needed, if some/any of ListenOptions changed. */
  def isScannerRestartNeeded(v0: IBleBeaconsApi.ListenOptions,
                             v2: IBleBeaconsApi.ListenOptions,
                             osFamily: Option[MOsFamily]): Boolean = {
    (v0.onBeacon eq v2.onBeacon) &&
    isScannerRestartNeededSettingsOnly(v0, v2, osFamily)
  }

  override def toString = getClass.getSimpleName

}


object IBleBeaconsApi extends Log {

  case class ListenOptions(
                            onBeacon          : BeaconDetected => Unit,
                            scanMode          : ScanMode,
                          )
  object ListenOptions {
    @inline implicit def univEq: UnivEq[ListenOptions] = UnivEq.force
  }


  type ScanMode = Int
  object ScanMode {
    /** Scan without scanning. Other unknown services/apps may start radio scan, and results may be popupalted into this beaconer. */
    final def OPPORTUNISTIC = 0.asInstanceOf[ScanMode]
    /** Scan with long pauses to save more energy. */
    final def LOW_POWER = 1.asInstanceOf[ScanMode]
    /** Scan and power-saving pauses nearby equal, balanced power-consumption. */
    final def BALANCED = 2.asInstanceOf[ScanMode]
    /** Scan without any pauses, maximum power consumption. */
    final def FULL_POWER = 3.asInstanceOf[ScanMode]
  }


  /** Find available API for beacon scan. */
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


