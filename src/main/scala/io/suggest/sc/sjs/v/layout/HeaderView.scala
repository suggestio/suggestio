package io.suggest.sc.sjs.v.layout

import io.suggest.sc.sjs.c.HeaderCtl
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Header._

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
  def showBackToIndexBtns(headerDiv: SafeEl[HTMLDivElement]): Unit = {
    headerDiv.addClasses(INDEX_ICON_CSS_CLASS)
  }

  /** Отключить отображение back to index. */
  def hideBackToIndexBtns(headerDiv: SafeEl[HTMLDivElement]): Unit = {
    headerDiv.removeClass(INDEX_ICON_CSS_CLASS)
  }


  /** Инициализация кнопки отображения панели поиска. */
  def initShowSearchPanelBtn(btnSafe: SafeEl[HTMLDivElement]): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        HeaderCtl.showSearchPanelBtnClick(e)
      }
    }
  }

  /** Инициализация кнопки сокрытия панели поиска.
    * @param btnSafe Кнопк сокрытия, на которую вешаем listener.
    */
  def initHideSearchPanelBtn(btnSafe: SafeEl[HTMLDivElement]): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      if ( !MTouchLock() ) {
        HeaderCtl.hideSearchPanelBtnClick(e)
      }
    }
  }


}
