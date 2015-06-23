package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.ScConstants.Search.{ITab, Cats, Nodes}
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.06.15 18:09
 * Description: DOM-модели для табов панели поиска.
 */

sealed trait MTabDom extends GetDivById {

  /** Модель id'шников из ScConstants. */
  def idModel: ITab

  /** div списка сущностей (категорий, магазинов, ...). */
  def rootDiv       = getDivById(idModel.ROOT_DIV_ID)

  /** div кнопки переключения на этот tab. */
  def tabBtnDiv     = getDivById(idModel.TAB_BTN_ID)

  def wrapperDiv    = getDivById(idModel.WRAPPER_DIV_ID)

  def contentDiv    = getDivById(idModel.CONTENT_DIV_ID)

}


/** DOM-модель таба категорий. */
object MCatsTab extends MTabDom {
  override def idModel = Cats
}

/** DOM-модель таба списка магазинов. */
object MNodesTab extends MTabDom {
  override def idModel = Nodes
}
