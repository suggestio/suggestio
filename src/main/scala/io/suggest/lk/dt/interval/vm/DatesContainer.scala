package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.ShowHideDisplayT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 18:30
 * Description: vm'ка контейнера с инпутами дат начала и конца при кастомном задании периода.
 */
object DatesContainer extends FindDiv {
  override type T     = DatesContainer
  override def DOM_ID = DatesIntervalConstants.DATES_CONT_ID
}


import io.suggest.lk.dt.interval.vm.DatesContainer.Dom_t


trait DsContainerT
  extends ShowHideDisplayT
  with IInitLayoutFsm
{
  override type T = Dom_t

  def start = StartVm.find()
  def end   = EndVm.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    start.foreach(f)
    end.foreach(f)
  }

}


case class DatesContainer(
  override val _underlying: Dom_t
)
  extends DsContainerT
