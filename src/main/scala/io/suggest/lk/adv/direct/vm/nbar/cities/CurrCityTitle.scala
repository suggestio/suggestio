package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindDiv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:00
 * Description: VM'ка заголовка текущего города.
 */
object CurrCityTitle extends FindDiv {
  override type T     = CurrCityTitle
  override def DOM_ID = AdvDirectFormConstants.CITY_CURR_TITLE_ID
}


import CurrCityTitle.Dom_t


trait CurrCityTitleT extends IVm with SetInnerHtml {

  override type T = Dom_t

}


case class CurrCityTitle(override val _underlying: Dom_t)
  extends CurrCityTitleT
