package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.ScConstants.Search._
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:48
 * Description: Доступ к DOM-элементам панели поиска.
 */
trait MSearchDomT extends GetDivById {

  @deprecated("Use SRoot.find() instead.", "2015.aug.5")
  def rootDiv         = getDivById(ROOT_DIV_ID)

  def ftsInput        = getElementById[HTMLInputElement](Fts.INPUT_ID)

  def ftsInputContainerDiv = getDivById(Fts.INPUT_CONTAINER_ID)

  def tabBtnsDiv      = getDivById(TAB_BTNS_DIV_ID)

  /** Вернуть все имеющиеся модели табов. */
  def mtabs           = List[MTabDom](MCatsTab, MNodesTab)

  def isPanelDisplayed(rootDiv: HTMLDivElement): Boolean = {
    rootDiv.style.display == "block"
  }

}

object MSearchDom extends MSearchDomT
