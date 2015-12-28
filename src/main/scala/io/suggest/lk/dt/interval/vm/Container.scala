package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindDiv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 22:51
 * Description: vm'ка контейнера.
 */
object Container extends FindDiv {
  override def DOM_ID = DatesIntervalConstants.CONT_ID
  override type T     = Container
}


import Container.Dom_t


trait ContainerT extends IVm with IInitLayoutFsm {

  override type T = Dom_t

  def datesCont = DatesContainer.find()
  def period    = PeriodVm.find()
  def info      = InfoContainer.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    datesCont.foreach(f)
    period.foreach(f)
  }

}


case class Container(override val _underlying: Dom_t)
  extends ContainerT
