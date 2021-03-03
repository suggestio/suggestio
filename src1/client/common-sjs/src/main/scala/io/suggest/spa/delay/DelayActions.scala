package io.suggest.spa.delay

import diode.Effect
import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:27
  * Description: Экшены [[ActionDelayerAh]] для
  */
sealed trait IDelayAction extends DAction

/** Команда к отправки экшена в отложенные. */
case class DelayAction(key: String, fx: Effect, delayMs: Int) extends IDelayAction

/** Сработал таймер отложенного экшена. */
private[delay] case class FireDelayedAction(key: String) extends IDelayAction

