package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.content.GetInnerHtml
import io.suggest.sjs.common.vm.find.FindElDynIdT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 14:10
 * Description: vm'ка для каждого заголовка таба города.
 */
object CityTabHead extends FindElDynIdT {

  override type Dom_t       = HTMLDivElement
  override type DomIdArg_t  = String
  override type T           = CityTabHead

  override def getDomId(arg: CityTabHead.DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_TAB_HEAD_ID(arg)
  }

}


import CityTabHead.Dom_t


trait CityTabHeadT extends IVm with GetInnerHtml with CityIdT {
  override type T = Dom_t
}


case class CityTabHead(override val _underlying: Dom_t)
  extends CityTabHeadT
