package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants.Tabs
import io.suggest.lk.adv.direct.m.CityNgIdOpt
import io.suggest.sjs.common.vm.attr.AttrVmT
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindElDynIdT
import org.scalajs.dom.raw.HTMLSpanElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 18:26
 * Description: VM'ка счетчиков таба.
 */
object TabCounter extends FindElDynIdT {
  override type Dom_t       = HTMLSpanElement
  override type T           = TabCounter
  override type DomIdArg_t  = CityNgIdOpt
  override def getDomId(arg: DomIdArg_t): String = {
    Tabs.COUNTER_ID(arg.cityId, arg.ngIdOpt)
  }
}

import TabCounter.Dom_t

trait TabCounterT extends SetInnerHtml with AttrVmT {

  override type T = Dom_t

  /** Стереть значение счетчика. */
  def unsetCounter(): Unit = {
    setContent("")
  }

  def maybeSetCounter(count: Int): Unit = {
    if (count > 0)
      setCounter(count)
    else
      unsetCounter()
  }

  /** Выставить значение счетчика. */
  def setCounter(count: Int): Unit = {
    setContent("(" + count + ")")
  }

  /** Прочитать значение аттрибута кол-ва доступных узлов в категории. */
  def totalAvail = getIntAttributeStrict(Tabs.TOTAL_AVAIL_ATTR)

}

case class TabCounter(override val _underlying: Dom_t)
  extends TabCounterT
