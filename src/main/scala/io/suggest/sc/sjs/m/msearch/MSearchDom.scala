package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.sjs.m.mdom.GetDivById
import io.suggest.sc.ScConstants.Search._
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:48
 * Description: Доступ к DOM-элементам панели поиска.
 */
object MSearchDom extends GetDivById {

  def rootDiv()       = getDivById(ROOT_DIV_ID)

  def ftsInput()      = getElementById[HTMLInputElement](FTS_FIELD_ID)

  def showPanelBtn()  = getDivById(SHOW_PANEL_BTN_ID)

  def tabBtnsDiv()    = getDivById(TAB_BTNS_DIV_ID)

  /** Вернуть все имеющиеся модели табов. */
  def mtabs()          = List[MTabDom](MCatsTab, MNodesTab)

}
