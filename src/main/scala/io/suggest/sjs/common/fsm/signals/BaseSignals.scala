package io.suggest.sjs.common.fsm.signals

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 15:39
  * Description: Файл с разными очень базовыми сигналами между различными FSM.
  */


/** Сигнал для завершения работы FSM-получателя. */
case class Stop()
  extends IFsmMsg


/** Сигнал видимости или невидимости контента, связанного с указанным FSM. */
case class Visible(isVisible: Boolean)
  extends IFsmMsg

