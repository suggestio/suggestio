package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.m.mhdr.MHeaderDom
import io.suggest.sc.sjs.m.msc.fsm.{MCatMeta, MScFsm}
import io.suggest.sc.sjs.v.layout.HeaderView
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 10:52
 * Description: Контроллер строки заголовка.
 */
object HeaderCtl extends CtlT with SjsLogger {

  /** Инициализация кнопок и прочего в строке заголовка. */
  def initLayout(): Unit = {
    // Инициализация кнопки показа/скрытия поисковой панели (справа).
    for (btn <- MHeaderDom.showSearchPanelBtn) {
      HeaderView.initShowSearchPanelBtn( SafeEl(btn) )
    }
    for (btn <- MHeaderDom.hideSearchPanelBtn) {
      HeaderView.initHideSearchPanelBtn( SafeEl(btn) )
    }
    for (btn <- MHeaderDom.showIndexBtn) {
      HeaderView.initShowIndexBtn( SafeEl(btn) )
    }

    // Инициализация кнопок управления навигационной панелью (слева).
    for (btn <- MHeaderDom.showNavPanelBtn) {
      HeaderView.initShowNavBtn( SafeEl(btn) )
    }
    for (btn <- MHeaderDom.hideNavPanelBtn) {
      HeaderView.initHideNavPanelBtn( SafeEl(btn) )
    }
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

  def showIndexBtnClick(e: Event): Unit = {
    MScFsm.transformState() {
      _.copy(
        searchPanelOpened = false,
        cat = None
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


  /** Юзер нажал кнопку показа панели навигации. */
  def showNavPanelBtnClick(e: Event): Unit = {
    MScFsm.transformState() {
      _.copy(
        navPanelOpened = true
      )
    }
  }

  /** Сокрытие корневых кнопок. Полезно, когда другой слой отображает собственные кнопки как бы на панели. */
  def hideRootBtns(): Unit = {
    for (btnsDiv <- MHeaderDom.btnsDiv) {
      HeaderView.hideBtns(btnsDiv)
    }
  }

  def showRootBtns(): Unit = {
    for (btnsDiv <- MHeaderDom.btnsDiv) {
      HeaderView.showBtns(btnsDiv)
    }
  }

  /** Юзер кликает по кнопке сокрытия панели навигации. */
  def hideNavPanelBtnClick(e: Event): Unit = {
    MScFsm.transformState() {
      _.copy(
        navPanelOpened = false
      )
    }
  }

}
