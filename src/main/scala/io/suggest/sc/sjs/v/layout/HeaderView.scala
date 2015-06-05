package io.suggest.sc.sjs.v.layout

import io.suggest.sc.sjs.c.HeaderCtl
import io.suggest.sc.sjs.m.msc.fsm.MCatMeta
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Header._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.15 9:48
 * Description: Представление строки заголовка выдачи.
 */
object HeaderView {

  /**
   * Как этот view должен реагировать на открытие панели поиска?
   * Он должен изменять набор отображаемых кнопок.
   * @param headerDiv корневой div строки заголовка.
   */
  def showBackToIndexBtns(headerDiv: SafeCssElT): Unit = {
    headerDiv.addClasses(INDEX_ICON_CSS_CLASS)
  }

  /** Отключить отображение back to index. */
  def hideBackToIndexBtns(headerDiv: SafeCssElT): Unit = {
    headerDiv.removeClass(INDEX_ICON_CSS_CLASS)
  }


  /** Инициализация кнопки отображения панели поиска. */
  def initShowSearchPanelBtn(btnSafe: SafeEventTargetT): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        HeaderCtl.showSearchPanelBtnClick(e)
      }
    }
  }

  /**
   * Инициализация списка кнопок сокрытия панели поиска.
   * @param btnsSafe Кнопки сокрытия, на которую вешаем listener.
   */
  def initHideSearchPanelBtn(btnsSafe: TraversableOnce[SafeEventTargetT]): Unit = {
    // Один инстанс листенера расшарен между всеми инициализируемыми кнопками.
    val listener: js.Function1[Event, _] = { e: Event =>
      if ( !MTouchLock() ) {
        HeaderCtl.hideSearchPanelBtnClick(e)
      }
    }
    for (btnSafe <- btnsSafe) {
      btnSafe.addEventListener(TouchUtil.clickEvtName)(listener)
    }
  }

  /** Привести класс категории к классу режима глобальной категории заголовока. */
  def catClass2hdrClass(catClass: String) = "__" + catClass
  
  /** Выставить новую глобальную категорию или удалить. */
  def updateGlobalCat(rootDiv: SafeEl[HTMLDivElement], catMeta: Option[MCatMeta], prevCatMeta: Option[MCatMeta]): Unit = {
    // Если была категория, а её не стало, то нужно спилить данные глобальной категории.
    for (pcm <- prevCatMeta) {
      rootDiv.removeClass( catClass2hdrClass(pcm.catClass) )
    }
    if (catMeta.isEmpty) {
      // Новое категорирование отключено. Отключить режим глобальной категории.
      rootDiv.removeClass(GLOBAL_CAT_CSS_CLASS)
    } else {
      val ncm = catMeta.get
      // Выставить класс для новой категории. Включить режим глобальное категории если необходимо.
      var classes = List( catClass2hdrClass(ncm.catClass) )
      if (prevCatMeta.isEmpty)
        classes ::= GLOBAL_CAT_CSS_CLASS
      rootDiv.addClasses( classes : _* )
    }
  }


  /** Инициализация кнопки показа панели навигации (слева). */
  def initShowNavBtn(btnSafe: SafeEventTargetT): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if (!MTouchLock()) {
        HeaderCtl.showNavPanelBtnClick(e)
      }
    }
  }

}
