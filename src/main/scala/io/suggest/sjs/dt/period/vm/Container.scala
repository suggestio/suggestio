package io.suggest.sjs.dt.period.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.interval.m.PeriodEith_t

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

    val _datesCont = datesCont
    _datesCont.foreach(f)

    val _period = period
    _period.foreach(f)

    // Скрыть/отобразить даты в зависимости от текущего выставленного значения периода
    for (p <- _period; dc <- _datesCont) {
      val isShown = p.isoPeriodOpt.isEmpty
      dc.setIsShown(isShown)
    }
  }

  /**
   * Прочитать текущее значение периода.
 *
   * @return None чтение невозможно
   *         Some(Left) период из дат.
   *         Some(Right) период-презет.
   */
  def getCurrPeriod: Option[PeriodEith_t] = {
    // Читаем селект.
    period.flatMap { periodSel =>
      periodSel.isoPeriodOpt.fold [Option[PeriodEith_t]] {
        for {
          dc <- datesCont
          dp <- dc.datesPeriodOpt
        } yield {
          Left(dp)
        }
      } { isoPeriod =>
        Some(Right(isoPeriod))
      }
    }
  }

}


case class Container(override val _underlying: Dom_t)
  extends ContainerT
