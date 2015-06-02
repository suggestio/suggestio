package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.v.search.SearchPanelView
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:24
 * Description: Контроллер реакции на события поисковой (правой) панели.
 */
object SearchPanelCtl extends CtlT with SjsLogger {

  /** Инициализация после загрузки выдачи узла. */
  def initNodeLayout(): Unit = {
    SearchPanelView.adjust()
    // Инициализация кнопки показа панели.
    for (btn <- MSearchDom.showPanelBtn()) {
      SearchPanelView.initShowSearchPanelBtn( SafeEl(btn) )
    }
    // Инициализация search-панели: реакция на кнопку закрытия/открытия, поиск при наборе, табы и т.д.
    for (input <- MSearchDom.ftsInput()) {
      SearchPanelView.initFtsField( SafeEl(input) )
    }
  }

  /** Клик по кнопке открытия поисковой панели. */
  def onShowPanelBtnClick(e: Event): Unit = {
    error("TODO")
  }

  def onFtsFieldFocus(e: Event): Unit = {
    error("TODO") // TODO
  }

  def onFtsFieldKeyUp(e: Event): Unit = {
    error("TODO") // TODO
  }

  def onFtsFieldBlur(e: Event): Unit = {
    error("TODO") // TODO
  }

}
