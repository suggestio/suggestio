package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.m.NgBodyId
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.lk.adv.direct.vm.nbar.nodes.{NodeRow, NodeCheckBox}
import io.suggest.lk.adv.direct.vm.nbar.tabs.{WithCityNgIdOpt, CityCatTab}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:57
 * Description: vm'ка тела одной группы нод.
 */
object CityCatNg extends FindElDynIdT with OfDiv {

  override type DomIdArg_t  = NgBodyId
  override type Dom_t       = HTMLDivElement
  override type T           = CityCatNg

  override def getDomId(cityCat: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_NODES_TAB_BODY_ID(cityCat.cityId, catId = cityCat.ngId)
  }

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id.startsWith( AdvDirectFormConstants.CITY_CAT_NODES_ID_PREFIX )
    }
  }

}


import CityCatNg.Dom_t


trait CityCatNgT
  extends WithCityNgIdOpt
  with ShowHideDisplayT
  with SetIsShown
{

  override type T = Dom_t

  def nodeRows: Iterator[NodeRow] = {
    DomListIterator(_underlying.children)
      .flatMap( NodeRow.ofElUnsafe )
  }

  def tabHead = _findByCityNgIdOpt(CityCatTab)

}


case class CityCatNg(override val _underlying: Dom_t)
  extends CityCatNgT
