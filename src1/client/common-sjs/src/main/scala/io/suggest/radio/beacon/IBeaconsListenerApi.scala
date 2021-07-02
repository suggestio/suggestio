package io.suggest.radio.beacon

import io.suggest.dev.MOsFamily
import io.suggest.log.Log
import io.suggest.radio.{MRadioSignalJs, MRadioSignalType}
import japgolly.univeq._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:21
  * Description: Interface for Bluetooth beacons radio scanning API.
  */

trait IBeaconsListenerApi {

  def radioSignalType: MRadioSignalType

  def doDispatch( signals: Seq[MRadioSignalJs],
                  opts: IBeaconsListenerApi.ListenOptions,
                ): Unit = {
    opts.dispatchEvent( RadioSignalsDetected(radioSignalType, signals) )
  }

  /** Is hardware/software/etc underlying stuff enabled at the moment? */
  def isPeripheralEnabled(): Future[Boolean]

  /** Try to enable/power-on related hardware/driver. */
  def enablePeripheral(): Future[Boolean]

  /** Is this API implementation available for using? */
  def isApiAvailable: Boolean

  /** Start to listen for beacon signals. */
  def listenBeacons(opts: IBeaconsListenerApi.ListenOptions): Future[_]

  /** Stop beacon listening. */
  def unListenAllBeacons(): Future[_]

  /** Is this scanner restart needed, if some settings changed?
    * Do not compared ListenOptions.onBeacon or other function instances. Only compare basic scan options. */
  def isScannerRestartNeededSettingsOnly(
                                          v0: IBeaconsListenerApi.ListenOptions,
                                          v2: IBeaconsListenerApi.ListenOptions,
                                          osFamily: Option[MOsFamily],
                                        ): Boolean

  /** Is scanner restart needed, if some/any of ListenOptions changed. */
  def isScannerRestartNeeded(v0: IBeaconsListenerApi.ListenOptions,
                             v2: IBeaconsListenerApi.ListenOptions,
                             osFamily: Option[MOsFamily]): Boolean = {
    (v0.dispatchEvent eq v2.dispatchEvent) &&
    isScannerRestartNeededSettingsOnly(v0, v2, osFamily)
  }

  override def toString = getClass.getSimpleName

}


object IBeaconsListenerApi extends Log {

  case class ListenOptions(
                            dispatchEvent     : IBeaconerAction => Unit,
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

  @inline implicit def univEq: UnivEq[IBeaconsListenerApi] = UnivEq.force

}
