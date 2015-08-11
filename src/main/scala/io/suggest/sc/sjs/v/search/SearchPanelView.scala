package io.suggest.sc.sjs.v.search

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.v.vutil.{OnClick, SetStyleDisplay, VUtil}
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sc.ScConstants.Search._
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:23
 * Description: Вьюшка для панели поиска.
 * В coffee-выдаче компонент жил внутри sm.navigation_layer.
 */
@deprecated("See vm.search package", "2015.aug.5")
object SearchPanelView extends SetStyleDisplay with OnClick {

  /** Как уточнить разметку панели. */
  @deprecated("Use SRoot.adjust() instead.", "2015.aug.5")
  def adjust(): Unit = {
    val offset: Int = if (MSearchDom.tabBtnsDiv.isEmpty) 100 else 150
    val height = MAgent.availableScreen.height - offset
    for (mtab <- MSearchDom.mtabs) {
      VUtil.setHeightRootWrapCont(height, mtab.contentDiv, mtab.rootDiv ++ mtab.wrapperDiv)
    }
  }

  /** Инициализировать кнопку таба. */
  @deprecated("Use TabBtn.initLayout() instead", "2015.aug.5")
  def initTabBtn(tabId: String, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    onClick(btnDiv) { e: Event =>
      SearchPanelCtl.onTabBtnClick(tabId, e)
    }
  }

  /** Инициализация списка категорий. При клике по активной категории должен активироваться поиск в категории.
    * Испольуется делегирование событий внешнему div'у. */
  def initCatsList(contentDiv: SafeEl[HTMLDivElement]): Unit = {
    onClick(contentDiv) { e: Event =>
      SearchPanelCtl.onCatLinkClick(e)
    }
  }

  /** Отобразить на экране панель поиска, которая, скорее всего, скрыта. */
  def showPanel(rootDiv: HTMLDivElement): Unit = {
    displayBlock(rootDiv)
  }

  /** Скрыть панель поиска. */
  def hidePanel(rootDiv: HTMLDivElement): Unit = {
    displayNone(rootDiv)
  }


  /** Показать указанный таб. */
  def showTab(tabRootDiv: HTMLDivElement, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    displayBlock(tabRootDiv)
    btnDiv.removeClass(TAB_BTN_INACTIVE_CSS_CLASS)
  }

  /** Скрыть указанный таб. */
  def hideTab(tabRootDiv: HTMLDivElement, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    displayNone(tabRootDiv)
    btnDiv.addClasses(TAB_BTN_INACTIVE_CSS_CLASS)
  }

}
