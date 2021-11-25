package io.suggest.radio.beacon

import io.suggest.radio.MRadioSignalType

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:09
  * Description: Actions foc controlling BeaconerAh.
  */


/** Result of subscription for IBleBeaconsApi events.
  * @param apisReady APIs map.
  */
private[beacon] case class HandleListenRes( apisReady: Map[MRadioSignalType, IBeaconsListenerApi] ) extends IBeaconerAction

/** Notify timer produced timeout. */
private[beacon] case class MaybeNotifyAll(timestamp: Long) extends IBeaconerAction


/** Action for garbage-collection inside beacons data. */
private[beacon] case object DoGc extends IBeaconerAction

/** Action to cleanup internal state after beaconer (de)initialization finished.
  * @param tryEnabled New isEnabled state.
  */
private[beacon] case class BtOnOffFinish(tryEnabled: Try[Boolean] ) extends IBeaconerAction
