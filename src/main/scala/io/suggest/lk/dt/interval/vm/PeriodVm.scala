package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.{DatesIntervalConstants, PeriodsConstants}
import io.suggest.lk.dt.interval.m.PeriodChangedEvent
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLSelectElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 14:05
 * Description: vm'ка для селектора периода дат.
 */
object PeriodVm extends FindElT {
  override type T     = PeriodVm
  override type Dom_t = HTMLSelectElement
  override def DOM_ID = DatesIntervalConstants.PERIOD_SELECT_ID
}


import PeriodVm.Dom_t


/** Логика экземпляра запрятана здесь. */
trait PeriodVmT
  extends InitLayoutFsmChange
{

  override type T = Dom_t

  /**
   * Прочитать значение периода.
   * @return Если выбран кастомный режим, то будет None.
   *         Иначе Some(ISO period).
   */
  def isoPeriodOpt: Option[String] = {
    Option( _underlying.value )
      .filter { _ != PeriodsConstants.CUSTOM }
  }

  override protected def _changeSignalModel = PeriodChangedEvent

}


/** Дефолтовая реализация класса vm'ки. */
case class PeriodVm(
  override val _underlying: Dom_t
)
  extends PeriodVmT
