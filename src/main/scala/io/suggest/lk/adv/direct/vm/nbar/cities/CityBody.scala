package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgHead
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:43
 * Description: Тело города, т.е. заголовки вкладок групп узлов.
 */
object CityBody extends FindElDynIdT {

  override type Dom_t       = HTMLDivElement
  override type DomIdArg_t  = String
  override type T           = CityBody

  override def getDomId(cityId: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_TAB_BODY_ID(cityId)
  }

}


import CityBody.Dom_t


trait CityBodyT extends CityIdT with ShowHideDisplayT with SetIsShown {

  override type T = Dom_t

  def ngHeads: Iterator[NgHead] = {
    for (el <- DomListIterator( _underlying.children )) yield {
      val el1 = el.asInstanceOf[NgHead.Dom_t]
      NgHead(el1)
    }
  }

}


case class CityBody(override val _underlying: Dom_t)
  extends CityBodyT
