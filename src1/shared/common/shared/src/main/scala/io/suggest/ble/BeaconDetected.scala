package io.suggest.ble

import io.suggest.spa.DAction
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.2020 15:57
  * Description: Экшен и модель с инфой по одному маячку.
  */

/** Интерфейс сигналов, принмаемых beaconer fsm. */
trait IBleBeaconAction extends DAction


/** Сигнал об обнаружении одного ble-маячка. */
case class BeaconDetected(
                           // TODO beacon - Заменить на EddyStone напрямую?
                           signal       : IBeaconSignal,
                           seenAtMs     : Long              = BeaconDetected.seenNowMs(),
                         )
  extends IBleBeaconAction

object BeaconDetected {

  final def IS_GONE_AFTER_SECONDS = 4

  def seenNowMs() = System.currentTimeMillis()

  implicit final class BcnDetectedOpsExt( private val bcnDetected: BeaconDetected ) extends AnyVal {

    /** Видимый ли сейчас? */
    def isStillVisibleNow(now: Long = seenNowMs()): Boolean = {
      (now - bcnDetected.seenAtMs)
        .milliseconds
        .toSeconds <= IS_GONE_AFTER_SECONDS
    }

  }

}