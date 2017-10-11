package io.suggest.spa.delay

import io.suggest.spa.DAction
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:27
  * Description: Экшены [[ActionDelayerAh]] для
  */
sealed trait IDelayAction extends DAction

/** Команда к отправки экшена в отложенные. */
object DelayAction {
  implicit def univEq: UnivEq[DelayAction] = UnivEq.derive
}
case class DelayAction(action: DAction, delayMs: Int) extends IDelayAction

/** Сработал таймер отложенного экшена. */
private[delay] case class FireDelayedAction(actionId: Int) extends IDelayAction

