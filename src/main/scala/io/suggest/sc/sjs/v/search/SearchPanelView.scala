package io.suggest.sc.sjs.v.search

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeEl
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
      VUtil.setHeightRootWrapCont(height, mtab.contentDiv(), mtab.rootDiv() ++ mtab.wrapperDiv())
    }
  }

  /** Инициализация кнопки отображения панели поиска. */
  def initShowPanelBtn(btnSafe: SafeEl[HTMLDivElement]): Unit = {
    btnSafe.addEventListener(TouchUtil.clickEvtName) { e: Event =>
      SearchPanelCtl.onShowPanelBtnClick(e)
    }
  }

  /** Инициализация кнопки сокрытия панели поиска. */
  def initHidePanelBtn(btnsSafe: SafeEl[HTMLDivElement]*): Unit = {
    val listener = { e: Event =>
      SearchPanelCtl.onHidePanelBtnClick(e)
    }
    for (btnSafe <- btnsSafe) {
      btnSafe.addEventListener(TouchUtil.clickEvtName)(listener)
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

  /** Отобразить на экране панель поиска, которая, скорее всего, скрыта. */
  def show(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "block"
  }

  def hide(rootDiv: HTMLDivElement): Unit = {
    rootDiv.style.display = "none"
  }

}
