package io.suggest.radio.beacon

import diode.data.Pot
import diode.{Effect, FastEq}
import io.suggest.ble.{BeaconsNearby_t, MUidBeacon}
import io.suggest.common.empty.EmptyProduct
import io.suggest.common.html.HtmlConstants
import io.suggest.radio.{MRadioData, MRadioSignalType}
import io.suggest.sjs.common.model.MTsTimerId
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:01
  * Description: Beaconer state-data container model.
  */
object MBeaconerS {

  def empty = MBeaconerS()

  @inline implicit def univEq: UnivEq[MBeaconerS] = UnivEq.force

  implicit object MBeaconerSFastEq extends FastEq[MBeaconerS] {
    override def eqv(a: MBeaconerS, b: MBeaconerS): Boolean = {
      (a.isEnabled ===* b.isEnabled) &&
        (a.afterOnOff eq b.afterOnOff) &&
        (a.notifyAllTimer ===* b.notifyAllTimer) &&
        (a.beacons ===* b.beacons) &&
        (a.nearbyReport ===* b.nearbyReport) &&
        (a.gcIntervalId ===* b.gcIntervalId) &&
        (a.envFingerPrint ===* b.envFingerPrint) &&
        (a.bleBeaconsApis ===* b.bleBeaconsApis) &&
        (a.opts ==* b.opts)
    }
  }

  def isEnabled = GenLens[MBeaconerS](_.isEnabled)
  def afterOnOff = GenLens[MBeaconerS]( _.afterOnOff )
  def notifyAllTimer = GenLens[MBeaconerS](_.notifyAllTimer)
  def beacons = GenLens[MBeaconerS](_.beacons)
  def gcIntervalId = GenLens[MBeaconerS](_.gcIntervalId)
  def nearbyReport = GenLens[MBeaconerS](_.nearbyReport)
  def opts = GenLens[MBeaconerS]( _.opts )
  def hasBle = GenLens[MBeaconerS]( _.hasBle )

}


/** Beaconer state-data container class.
  * Contains runtime state about BLE beacons scanning, and other data.
  *
  * @param isEnabled Is scanning enabled now?
  *                  Pot.empty - now BLE API not available on device or not yet tested.
  *                  true - Enabled now. true+pending - Enabling in progress.
  *                  false - Disabled. false+pending - Disabling in progress.
  * @param afterOnOff After enabling/disablig, this effect must be executed.
  * @param beacons Current beacons scan state (map by beacon id).
  * @param envFingerPrint Beacons scan state current state hash.
  * @param bleBeaconsApis Current active BLE API.
  *                      Pot.empty - no initialized API, at the moment.
  *                      Pending - API initialization going in background.
  *                      Error - API failed to initialize.
  * @param gcIntervalId timer id for GC in beacons.
  * @param nearbyReport Overall report about currently visible beacons.
  * @param hasBle Has BLE scan support on this device?
  *               Can be checked after Cordova PLATFORM_READY
  *               Pot.empty - not checked, by now.
  *               Ready(true|false) - Detected bluetooth support.
  *               failed(ex) - Detection failed.
  */
case class MBeaconerS(
                       isEnabled            : Pot[Boolean]               = Pot.empty,
                       afterOnOff           : Option[Effect]             = None,
                       notifyAllTimer       : Option[MTsTimerId]         = None,
                       beacons              : Map[String, MRadioData]   = Map.empty,
                       nearbyReport         : BeaconsNearby_t            = Nil,
                       gcIntervalId         : Option[Int]                = None,
                       envFingerPrint       : Option[Int]                = None,
                       bleBeaconsApis       : Pot[Map[MRadioSignalType, IBeaconsListenerApi]] = Pot.empty,
                       opts                 : MBeaconerOpts              = MBeaconerOpts.default,
                       hasBle               : Pot[Boolean]               = Pot.empty,
                     )
  extends EmptyProduct
{

  /** nearbyReport, indexed by beacon id. */
  lazy val nearbyReportById: Map[String, MUidBeacon] =
    nearbyReport
      .zipWithIdIter
      .toMap

  /** Minimized toString implementation. */
  override final def toString: String = {
    import HtmlConstants._

    new StringBuilder(128, productPrefix)
      .append( `(` )
      .append( isEnabled ).append( COMMA )
      .append( notifyAllTimer )
      // Instead of full beacons map rendering, generate simplified string.
      .append( beacons.size ).append( COLON )
      .append( `[` )
      .appendAll(
        beacons
          .keysIterator
          .flatMap { bcnId =>
            // Generate string like "02...43A5"
            bcnId.substring(0, 2) ::
              HtmlConstants.ELLIPSIS ::
              bcnId.substring(bcnId.length - 4, bcnId.length) ::
              COMMA ::
              Nil
          }
          .flatMap(_.toCharArray)
      )
      .append( `]` ).append( COMMA )
      // nearbyReport: render only hash-length, not full hash.
      .append( nearbyReport.length ).append( DIEZ ).append( COMMA )
      .append( gcIntervalId ).append( COMMA )
      .append( envFingerPrint ).append( COMMA )
      .append( bleBeaconsApis ).append( COMMA )
      .append( opts )
      .append( `)` )
      .toString()
  }

}

