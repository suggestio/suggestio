package io.suggest.sjs.dt.period.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.dt.period.m.EndChangedEvt

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 18:50
 * Description: vm'ка для ввода даты окончания периода при кастомном периоде.
 */
object EndVm extends DateStaticVmT {
  override type T     = EndVm
  override def DOM_ID = DatesIntervalConstants.DATE_END_INPUT_ID
}


import EndVm.Dom_t


case class EndVm(override val _underlying: Dom_t) extends DateVmT {

  override protected def _changeSignalModel = EndChangedEvt
}
