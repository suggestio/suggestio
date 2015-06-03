package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.mgrid.{MGridDom, MGridState, MGrid}
import io.suggest.sc.sjs.m.msc.MHeaderDom
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.v.layout.HeaderView
import io.suggest.sc.sjs.v.search.SearchPanelView
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:24
 * Description: Контроллер реакции на события поисковой (правой) панели.
 */
object SearchPanelCtl extends CtlT with SjsLogger with GridOffsetSetter {

  /** Инициализация после загрузки выдачи узла. */
  def initNodeLayout(): Unit = {
    SearchPanelView.adjust()

    // Инициализация кнопки показа панели.
    for (btn <- MSearchDom.showPanelBtn) {
      SearchPanelView.initShowPanelBtn( SafeEl(btn) )
    }

    // Инициализация search-панели: реакция на кнопку закрытия/открытия, поиск при наборе, табы и т.д.
    for (input <- MSearchDom.ftsInput) {
      SearchPanelView.initFtsField( SafeEl(input) )
    }

    // Инициализация кнопок сокрытия панели
    SearchPanelView.initHidePanelBtn {
      (MSearchDom.hidePanelBtn ++ MHeaderDom.showIndexBtn)
        .iterator
        .map(SafeEl.apply)
    }

    // Инициализация кнопок переключения табов поиска.
    for {
      mtab    <- MSearchDom.mtabs
      btnDiv  <- mtab.tabBtnDiv
    } {
      SearchPanelView.initTabBtn(mtab.idModel.ROOT_DIV_ID, SafeEl(btnDiv))
    }
  }

  /** Реакция на клик по кнопке открытия поисковой панели. */
  def onShowPanelBtnClick(e: Event): Unit = {
    // TODO Здесь был вызов adjust(), но есть сомнения, что он необходим.
    val rootDivOpt = MSearchDom.rootDiv
    for (rootDiv <- rootDivOpt if !MSearchDom.isPanelDisplayed(rootDiv)) {
      // Скрыть кнопки хидера главного экрана
      for (headerDiv <- MHeaderDom.rootDiv) {
        HeaderView.showBackToIndexBtns( SafeEl(headerDiv) )
      }
      SearchPanelView.show(rootDiv)
      maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(false))
    }
  }


  /** Реакция на клик по какой-либо кнопке сокрытия поисковой панели. */
  def onHidePanelBtnClick(e: Event): Unit = {
    val rootDivOpt = MSearchDom.rootDiv
    for (rootDiv <- rootDivOpt if MSearchDom.isPanelDisplayed(rootDiv)) {
      // Скрыть на хидере главного экрана кнопки сокрытия панели
      for (headerDiv <- MHeaderDom.rootDiv) {
        HeaderView.hideBackToIndexBtns( SafeEl(headerDiv) )
      }
      SearchPanelView.hide(rootDiv)
      maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(true))
    }
  }

  /** Если ширина экрана позволяет, то выставить сетке новый rightOffset и отребилдить. */
  def maybeRebuildGrid(rootDivOpt   : Option[HTMLDivElement]  = MSearchDom.rootDiv,
                       isHiddenOpt  : Option[Boolean]         = None,
                       _mgs         : MGridState              = MGrid.state): Unit = {
    // на мобиле выдачу не надо перекорчевывать, она остается под панелью. На экранах по-шире выдача "сдвигается".
    if (_mgs.isDesktopView) {
      // Облегченный offset-калькулятор, которому ничего толком искать не надо (всё уже найдено)
      val calc = new GridOffsetCalc {
        override def mgs = _mgs
        override def elOpt = rootDivOpt
        override def isElHidden(el: HTMLElement): Boolean = {
          isHiddenOpt getOrElse super.isElHidden(el)
        }
      }
      calc.execute()
      for (containerDiv <- MGridDom.containerDiv) {
        GridCtl.resetContainerSz(containerDiv)
      }
      GridCtl.build(isAdd = false, withAnim = true)
    }
  }

  /**
   * Клик по кнопке переключения на указанную вкладку.
   * @param id id вкладки, на которую происходит переключение.
   */
  def onTabBtnClick(id: String, e: Event): Unit = {
    for {
      mtab    <- MSearchDom.mtabs
      rootDiv <- mtab.rootDiv
      btnDiv  <- mtab.tabBtnDiv.map( SafeEl.apply )
    } {
      if (mtab.idModel.ROOT_DIV_ID == id) {
        // Это целевая вкладка. Отобразить её.
        SearchPanelView.showTab(rootDiv, btnDiv = btnDiv)
      } else {
        // [Теперь] эта вкладка неактивна.
        SearchPanelView.hideTab(rootDiv, btnDiv = btnDiv)
      }
    }
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


  /** Трейт для сборки считалок-обновлялок grid offsets для search-панели. */
  trait GridOffsetCalc extends super.GridOffsetCalc {
    override def elOpt    = MSearchDom.rootDiv
    override def minWidth = 300
    override def setOffset(newOff: Int): Unit = {
      mgs.rightOffset = newOff
    }
  }

}
