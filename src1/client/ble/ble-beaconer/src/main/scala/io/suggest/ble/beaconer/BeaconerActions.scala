package io.suggest.ble.beaconer

import diode.data.Pot
import io.suggest.ble.IBleBeaconAction
import io.suggest.ble.api.IBleBeaconsApi

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
                   opts: MBeaconerOpts = MBeaconerOpts.default) extends IBleBeaconAction

/** Result of subscription for IBleBeaconsApi events. */
private[beaconer] case class HandleListenRes( listenTryRes: Try[IBleBeaconsApi] ) extends IBleBeaconAction

/** Notify timer produced timeout. */
private[beaconer] case class MaybeNotifyAll(timestamp: Long) extends IBleBeaconAction


/** Action for garbage-collection inside beacons data. */
private[beaconer] case object DoGc extends IBleBeaconAction

/** Action to cleanup internal state after beaconer (de)initialization finished.
  * @param tryEnabled New isEnabled state.
  */
private[beaconer] case class BtOnOffFinish(tryEnabled: Try[Boolean] ) extends IBleBeaconAction


/** Result for testing about bluetooth abilities presence. */
private[beaconer] case class HasBleRes( hasBle: Pot[Boolean] ) extends IBleBeaconAction
