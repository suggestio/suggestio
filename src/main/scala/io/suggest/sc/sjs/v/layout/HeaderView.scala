package io.suggest.sc.sjs.v.layout

import io.suggest.sc.sjs.c.HeaderCtl
import io.suggest.sc.sjs.m.msc.fsm.MCatMeta
import io.suggest.sc.sjs.v.vutil.{OnClick, SetStyleDisplay}
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Header._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.15 9:48
 * Description: Представление строки заголовка выдачи.
 */
object HeaderView extends SetStyleDisplay with OnClick {

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
    onClick(btnSafe) { e: Event =>
      HeaderCtl.showSearchPanelBtnClick(e)
    }
  }

  /**
   * Инициализация списка кнопок сокрытия панели поиска.
   * @param btnSafe Кнопка сокрытия панели.
   */
  def initHideSearchPanelBtn(btnSafe: SafeEventTargetT): Unit = {
    onClick(btnSafe) { e: Event =>
      HeaderCtl.hideSearchPanelBtnClick(e)
    }
  }
  
  def initShowIndexBtn(btnSafe: SafeEventTargetT): Unit = {
    onClick(btnSafe) { e: Event =>
      HeaderCtl.showIndexBtnClick(e)
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
    onClick(btnSafe) { e: Event =>
      HeaderCtl.showNavPanelBtnClick(e)
    }
  }

  /** Инициализация событий кнопки сокрытия панели навигации. */
  def initHideNavPanelBtn(btnSafe: SafeEventTargetT): Unit = {
    onClick(btnSafe) { e: Event =>
      HeaderCtl.hideNavPanelBtnClick(e)
    }
  }

  /** Скрыть все базовые кнопки строки заголовка. */
  def hideBtns(btnsDiv: HTMLDivElement): Unit = {
    displayNone(btnsDiv)
  }

  /** Показать базовые кнопки строки заголовка. */
  def showBtns(btnsDiv: HTMLDivElement): Unit = {
    displayBlock(btnsDiv)
  }

}
