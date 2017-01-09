package io.suggest.lk.adn.map.m

import io.suggest.sjs.interval.m.PeriodEith_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 19:00
  * Description: State data для [[io.suggest.lk.adn.map.fsm.LamFormFsm]].
  */
case class MLamFsmSd(
  period          : PeriodEith_t,
  getPriceTs      : Option[Long] = None
)
