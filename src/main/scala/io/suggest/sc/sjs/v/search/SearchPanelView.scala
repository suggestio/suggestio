package io.suggest.sc.sjs.v.search

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sc.ScConstants.Search._
import io.suggest.sjs.common.view.safe.css.SafeCssEl
import org.scalajs.dom.{Node, Element, Event}
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

  /** Инициализация кнопки отображения панели поиска. */
  def initShowPanelBtn(btnSafe: SafeEl[HTMLDivElement]): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        SearchPanelCtl.onShowPanelBtnClick(e)
      }
    }
  }

  /** Инициализация кнопки сокрытия панели поиска.
    * @param btnsSafe Все кнопки сокрытия, на которые надо повесить listener.
    */
  def initHidePanelBtn(btnsSafe: TraversableOnce[SafeEl[HTMLDivElement]]): Unit = {
    // Шарим инстанс листенера между субъектами, чтобы сэкномить капельку RAM.
    val listener = { e: Event =>
      if ( !MTouchLock() ) {
        SearchPanelCtl.onHidePanelBtnClick(e)
      }
    }
    for (btnSafe <- btnsSafe) {
      btnSafe.addEventListener(TouchUtil.clickEvtName)(listener)
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
        // Найти основной div ссылки категории: он помечен классом js-cat-link.
        val clickedNode = e.target.asInstanceOf[Node]
        if (clickedNode != null) {
          for (catElSafe <- VUtil.hasCssClass(SafeCssEl(clickedNode), Cats.ONE_CAT_LINK_CSS_CLASS)) {
            // TODO Выпилить тут каст к Element.
            val catEl = catElSafe._underlying.asInstanceOf[Element]
            for (catId <- VUtil.getAttribute(catEl, Cats.ATTR_CAT_ID)) {
              SearchPanelCtl.onCatLinkClick(catId, e)
            }
          }
        }
      }
    }
  }

  /** Отобразить на экране панель поиска, которая, скорее всего, скрыта. */
  def showPanel(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "block"
  }

  /** Скрыть панель поиска. */
  def hidePanel(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "none"
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
