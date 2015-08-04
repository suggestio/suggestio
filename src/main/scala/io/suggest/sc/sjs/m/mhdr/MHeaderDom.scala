package io.suggest.sc.sjs.m.mhdr

import io.suggest.sc.ScConstants.Header._
import io.suggest.sc.ScConstants.{Search, NavPane}
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:56
 * Description: Модель для доступа к dom-элементам заголовка: контейнерам, кнопкам и т.д.
 */
trait MHeaderDomT extends GetDivById {

  @deprecated("Use HRoot instead", "2015.aug.4")
  def rootDiv               = getDivById(ROOT_DIV_ID)

  @deprecated("Use HShowIndexBtn instead.", "2015.aug.4")
  def showIndexBtn          = getDivById(SHOW_INDEX_BTN_ID)

  @deprecated("Use HHideSearchPanelBtn instead.", "2015.aug.4")
  def hideSearchPanelBtn    = getDivById( Search.HIDE_PANEL_BTN_ID )

  @deprecated("Use HShowSearchPanelBtn instead.", "2015.aug.4")
  def showSearchPanelBtn    = getDivById( Search.SHOW_PANEL_BTN_ID )

  @deprecated("Use HShowNavPanelBtn instead.", "2015.aug.4")
  def showNavPanelBtn       = getDivById( NavPane.SHOW_PANEL_BTN_ID )

  @deprecated("Use HHideNavBtn instead.", "2015.aug.4")
  def hideNavPanelBtn       = getDivById( NavPane.HIDE_PANEL_BTN_ID )
  
  def btnsDiv               = getDivById( BTNS_DIV_ID )

}

object MHeaderDom extends MHeaderDomT
