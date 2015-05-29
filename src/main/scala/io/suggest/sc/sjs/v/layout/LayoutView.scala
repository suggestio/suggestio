package io.suggest.sc.sjs.v.layout

import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.m.SafeDoc
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.msc.{MLayoutDom, MRedrawLayoutResult}
import io.suggest.sc.sjs.v.res.{CommonRes, FocusedRes}
import io.suggest.sc.sjs.v.vutil.VUtil
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:09
 * Description: Управление представлением корневых элементов выдачи.
 */
object LayoutView {

  /** Перерисовать layout выдачи. */
  def redrawLayout(oldRootDivOpt: Option[HTMLDivElement] = MLayoutDom.rootDiv()): MRedrawLayoutResult = {
    // Вычистить предыдущуй layout и его ресурсы:
    CommonRes.recreate()
    FocusedRes.recreate()

    // Удалить старый root div.
    if (oldRootDivOpt.nonEmpty) {
      val el = oldRootDivOpt.get
      el.parentNode
        .removeChild(el)
    }

    // TODO sm.geo.active_layer = null   // Выставить текущий выбранный слой в колонке навигации по узлам в состоянии.

    // Собрать новый пустой layout:
    val rootDiv = VUtil.newDiv()
    rootDiv.setAttribute("id", ROOT_ID)
    rootDiv.setAttribute("class", ROOT_CSS_CLASS)
    rootDiv.style.display = "none"

    val layoutDiv = VUtil.newDiv()
    layoutDiv.setAttribute("id", LAYOUT_ID)

    rootDiv.appendChild(layoutDiv)

    SafeDoc.body.appendChild(rootDiv)
    
    MRedrawLayoutResult(rootDiv = rootDiv, layoutDiv = layoutDiv)
  }


  /** Из-за прозрачностей нужно очистить фон до блеска после приветствия. */
  def eraseBg(rootDiv: HTMLDivElement): Unit = {
    def _erase(el: HTMLElement): Unit = {
      el.style.backgroundColor = "#ffffff"
    }
    _erase( SafeDoc.body )
    _erase(rootDiv)
  }

  /** Выставить css-класс отображения для layoutDiv. */
  def setWndClass(layoutDiv: HTMLDivElement): Unit = {
    val w = MAgent.availableScreen.width
    val cssClassOrNull: String = {
      if (w <= 660) {
        "sm-w-400"
      } else if (w <= 800) {
        "sm-w-800"
      } else if (w <= 980) {
        "sm-w-980"
      } else {
        null
      }
    }
    if (cssClassOrNull != null) {
      layoutDiv.className = cssClassOrNull
    }
  }


  /**
   * Получены новые данные node index. Нужно стереть старый layout, впилить новый.
   * @param indexHtml верстка index-страницы.
   * @return void, когда всё закончится.
   */
  def showIndex(indexHtml: String, rootDiv: HTMLDivElement, layoutDiv: HTMLDivElement): Unit = {
    // TODO bind_window_events() - реагировать на ресайз. Но это наверное должно происходить уровнем выше.
    dom.window.scrollTo(0, 0)

    rootDiv.style.display = "block"
    layoutDiv.innerHTML = indexHtml
    SafeDoc.body.style.overflow = "hidden"
  }

}
