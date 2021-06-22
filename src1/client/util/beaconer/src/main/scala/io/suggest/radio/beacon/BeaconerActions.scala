package io.suggest.radio.beacon

import diode.data.Pot
import io.suggest.radio.MRadioSignalType

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:09
  * Description: Actions foc controlling BeaconerAh.
  */


/** Start/stop or alter options of bluetooth BeaconerAh.
  *
  * @param isEnabled New state (on/off)
  *                  None means stay in current state, possibly altering beaconer options.
  * @param opts Options of beaconer.
  */
case class BtOnOff(isEnabled: Option[Boolean],
                   opts: MBeaconerOpts = MBeaconerOpts.default) extends IBeaconAction


/** Result of subscription for IBleBeaconsApi events.
  * @param apisReady APIs map.
  */
private[beacon] case class HandleListenRes( apisReady: Map[MRadioSignalType, IBeaconsListenerApi] ) extends IBeaconAction

/** Notify timer produced timeout. */
private[beacon] case class MaybeNotifyAll(timestamp: Long) extends IBeaconAction


/** Action for garbage-collection inside beacons data. */
private[beacon] case object DoGc extends IBeaconAction

/** Action to cleanup internal state after beaconer (de)initialization finished.
  * @param tryEnabled New isEnabled state.
  */
private[beacon] case class BtOnOffFinish(tryEnabled: Try[Boolean] ) extends IBeaconAction


/** Result for testing about bluetooth abilities presence. */
private[beacon] case class HasBleRes(hasBle: Pot[Boolean] ) extends IBeaconAction
