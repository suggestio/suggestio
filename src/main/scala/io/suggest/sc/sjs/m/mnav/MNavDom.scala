package io.suggest.sc.sjs.m.mnav

import io.suggest.sc.ScConstants.NavPane._
import io.suggest.sc.sjs.m.mdom.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:25
 * Description: Доступ к DOM-элементам, связанным с панелью навигации.
 */
trait MNavDomT extends GetDivById {

  def rootDiv           = getDivById(ROOT_ID)

  def nodeListDiv       = getDivById(NODE_LIST_ID)

  def wrapperDiv        = getDivById(WRAPPER_ID)

  def contentDiv        = getDivById(CONTENT_ID)

  def showPanelBtn      = getDivById(SHOW_PANE_BTN_ID)

}


object MNavDom extends MNavDomT
