package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.lk.dt.interval.m.StartChangedEvt

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 18:39
 * Description: vm'ка инпута даты начала размещения.
 */
object StartVm extends DateStaticVmT {
  override type T     = StartVm
  override def DOM_ID = DatesIntervalConstants.DATE_START_INPUT_ID
}


import StartVm.Dom_t


case class StartVm(override val _underlying: Dom_t) extends DateVmT {

  override protected def _changeSignalModel = StartChangedEvt
}
