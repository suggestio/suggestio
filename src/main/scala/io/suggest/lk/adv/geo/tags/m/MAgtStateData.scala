package io.suggest.lk.adv.geo.tags.m

import io.suggest.sjs.interval.m.PeriodEith_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:32
  * Description: Контейнер данных состояния FSM формы размещения в геотегах.
 *
  * @see [[io.suggest.lk.tags.edit.fsm.TagsEditFsm]] FSM.
  */
case class MAgtStateData(
  period          : PeriodEith_t,
  getPriceTs      : Option[Long] = None
)
