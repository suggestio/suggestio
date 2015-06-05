package io.suggest.sc.sjs.v.search

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sc.ScConstants.Search._
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:23
 * Description: Вьюшка для панели поиска.
 * В coffee-выдаче компонент жил внутри sm.navigation_layer.
 */
object SearchPanelView {

  /** Как уточнить разметку панели. */
  def adjust(): Unit = {
    val offset: Int = if (MSearchDom.tabBtnsDiv.isEmpty) 100 else 150
    val height = MAgent.availableScreen.height - offset
    for (mtab <- MSearchDom.mtabs) {
      VUtil.setHeightRootWrapCont(height, mtab.contentDiv, mtab.rootDiv ++ mtab.wrapperDiv)
    }
  }

  /** Инициализировать кнопку таба. */
  def initTabBtn(tabId: String, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    btnDiv.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        SearchPanelCtl.onTabBtnClick(tabId, e)
      }
    }
  }

  /** Инициализация поля полнотекстового поиска. */
  def initFtsField(fieldSafe: SafeEl[HTMLInputElement]): Unit = {
    // Навешиваем события
    fieldSafe.addEventListener("keyup") { (e: Event) =>
      SearchPanelCtl.onFtsFieldKeyUp(e)
    }
    fieldSafe.addEventListener("focus") { (e: Event) =>
      SearchPanelCtl.onFtsFieldFocus(e)
    }
    fieldSafe.addEventListener("blur") { (e: Event) =>
      SearchPanelCtl.onFtsFieldBlur(e)
    }
  }

  /** Инициализация списка категорий. При клике по активной категории должен активироваться поиск в категории.
    * Испольуется делегирование событий внешнему div'у. */
  def initCatsList(contentDiv: SafeEl[HTMLDivElement]): Unit = {
    contentDiv.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        SearchPanelCtl.onCatLinkClick(e)
      }
    }
  }

  /** Отобразить на экране панель поиска, которая, скорее всего, скрыта. */
  def showPanel(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "block"
  }
  def showPanel(): Unit = {
    for (spRootDiv <- MSearchDom.rootDiv) {
      showPanel(spRootDiv)
    }
  }

  /** Скрыть панель поиска. */
  def hidePanel(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "none"
  }
  def hidePanel(): Unit = {
    for (spRootDiv <- MSearchDom.rootDiv) {
      hidePanel(spRootDiv)
    }
  }

  /** Показать указанный таб. */
  def showTab(tabRootDiv: HTMLDivElement, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    tabRootDiv.style.display = "block"
    btnDiv.removeClass(TAB_BTN_INACTIVE_CSS_CLASS)
  }

  /** Скрыть указанный таб. */
  def hideTab(tabRootDiv: HTMLDivElement, btnDiv: SafeEl[HTMLDivElement]): Unit = {
    tabRootDiv.style.display = "none"
    btnDiv.addClasses(TAB_BTN_INACTIVE_CSS_CLASS)
  }

}
