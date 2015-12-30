package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.{VmT, IVm}
import io.suggest.sjs.common.vm.find.FindElDynIdT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:41
 * Description: Заголовок одной вкладки.
 */
object NgHead extends FindElDynIdT {

  override type Dom_t       = HTMLDivElement
  override type T           = NgHead
  override type DomIdArg_t  = (String, Option[String])    // cityId, Option(catId)

  override def getDomId(cityCat: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_NODES_TAB_HEAD_ID(cityCat._1, cityCat._2)
  }

  def of(vm: VmT): Option[NgHead] = {
    for (vmH <- VUtil.hasCssClass(vm, AdvDirectFormConstants.NGRP_TAB_HEAD_CLASS)) yield {
      val elH = vmH._underlying.asInstanceOf[Dom_t]
      apply(elH)
    }
  }

}


import NgHead.Dom_t


trait NgHeadT extends IVm with CityIdT with NgIdT {
  override type T = Dom_t
}


case class NgHead(override val _underlying: Dom_t)
  extends NgHeadT
