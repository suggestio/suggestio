package io.suggest.sc.sjs.m.mhdr

import io.suggest.sc.ScConstants.Header._
import io.suggest.sc.ScConstants.{Search, NavPane}
import io.suggest.sc.sjs.m.mdom.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:56
 * Description: Модель для доступа к dom-элементам заголовка: контейнерам, кнопкам и т.д.
 */
trait MHeaderDomT extends GetDivById {

  def rootDiv               = getDivById(ROOT_DIV_ID)

  def showIndexBtn          = getDivById(SHOW_INDEX_BTN_ID)

  def hideSearchPanelBtn    = getDivById( Search.HIDE_PANEL_BTN_ID )

  def showSearchPanelBtn    = getDivById( Search.SHOW_PANEL_BTN_ID )

  def showNavPanelBtn       = getDivById( NavPane.SHOW_PANEL_BTN_ID )

  def hideNavPanelBtn       = getDivById( NavPane.HIDE_PANEL_BTN_ID )
  
  def btnsDiv               = getDivById( BTNS_DIV_ID )

}

object MHeaderDom extends MHeaderDomT
