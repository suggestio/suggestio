package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.adv.direct.AdvDirectFormConstants.NgBodyId
import io.suggest.lk.adv.direct.vm.nbar.nodes.NodeRow
import io.suggest.lk.adv.direct.vm.nbar.tabs.{CityCatTab, WithCityNgIdOpt}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdToString, OfHtmlElDomIdRelated}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:57
 * Description: vm'ка тела одной группы нод.
 */

object CityCatNg
  extends FindElDynIdT
    with OfDiv
    with DynDomIdToString
    with DomIdPrefixed
    with OfHtmlElDomIdRelated
{

  override type DomIdArg_t  = NgBodyId
  override type T           = CityCatNg
  override def DOM_ID_PREFIX = AdvDirectFormConstants.CITY_CAT_NODES_ID_PREFIX

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
