package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.m.mhdr.MHeaderDom
import io.suggest.sc.sjs.m.msc.fsm.{MCatMeta, MScFsm}
import io.suggest.sc.sjs.v.layout.HeaderView
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:52
 * Description: Контроллер строки заголовка.
 */
object HeaderCtl extends CtlT {

  /** Инициализация кнопок и прочего в строке заголовка. */
  def initLayout(): Unit = {
    // Инициализация кнопки показа панели.
    for (btn <- MHeaderDom.showSearchPanelBtn) {
      HeaderView.initShowSearchPanelBtn( SafeEl(btn) )
    }

    // Инициализация кнопок сокрытия панели
    for(btn <- MHeaderDom.hideSearchPanelBtn) {
      HeaderView.initHideSearchPanelBtn( SafeEl(btn) )
    }

    // TODO MHeaderDom.showIndexBtn
  }


  /** Реакция на клик по кнопке открытия поисковой панели. */
  def showSearchPanelBtnClick(e: Event): Unit = {
    MScFsm.transformState() {
      _.copy(
        searchPanelOpened = true,
        cat = None
      )
    }
  }


  /** Реакция на клик по какой-либо кнопке сокрытия поисковой панели. */
  def hideSearchPanelBtnClick(e: Event): Unit = {
    MScFsm.transformState() {
      _.copy(
        searchPanelOpened = false
      )
    }
  }

  /**
   * Экшен замены глобальной категории.
   * @param catMeta Новое состояние категории.
   * @param prevCatMeta Старое состояние категории.
   */
  def changeGlobalCat(catMeta: Option[MCatMeta], prevCatMeta: Option[MCatMeta]): Unit = {
    for (headerDiv <- MHeaderDom.rootDiv) {
      val safeEl = SafeEl(headerDiv)
      HeaderView.updateGlobalCat(safeEl, catMeta, prevCatMeta = prevCatMeta)
    }
  }


}
