package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.lk.adv.direct.vm.nbar.nodes.NodeCheckBox
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:57
 * Description: vm'ка тела одной группы нод.
 */
object NgBody extends FindElDynIdT {

  override type DomIdArg_t  = (String, String)   // cityId, catId
  override type Dom_t       = HTMLDivElement
  override type T           = NgBody

  override def getDomId(cityCat: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_NODES_TAB_BODY_ID(cityCat._1, cityCat._2)
  }

}


import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgBody.Dom_t


trait NgBodyT extends IVm with CityIdT with NgIdT with ShowHideDisplayT with SetIsShown {

  override type T = Dom_t

  // TODO Node rows тут нужно итерировать, а уже внутри него чекбоксы!!

  // TODO Нужно считывать nodeId, затем по id искать input checkbox.
  def checkBoxes = {
    DomListIterator(_underlying.getElementsByTagName("input"))
      .filter { _.asInstanceOf[HTMLInputElement].`type`.equalsIgnoreCase("checkbox") }
      .toSeq
      .map { el =>
        val el1 = el.asInstanceOf[NodeCheckBox.Dom_t]
        NodeCheckBox(el1)
      }
  }

}


case class NgBody(override val _underlying: Dom_t)
  extends NgBodyT
