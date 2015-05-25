package io.suggest.sc.sjs.v.layout

import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.m.SafeDoc
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.res.{CommonRes, FocusedRes}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:09
 * Description: Управление представлением корневых элементов выдачи.
 */
object Layout {

  /**
   * Перерисовать layout выдачи.
   */
  def reDrawLayout()(implicit vctx: IVCtx): Unit = {
    // Вычистить предыдущуй layout и его ресурсы:
    CommonRes.recreate()
    FocusedRes.recreate()

    // Удалить старый root div.
    val lctx = vctx.layout
    val oldRoot = lctx.rootDiv.get
    if (oldRoot.nonEmpty) {
      val el = oldRoot.get
      el.parentNode
        .removeChild(el)
    }

    // TODO sm.geo.active_layer = null   // Выставить текущий выбранный слой в колонке навигации по узлам в состоянии.

    // Собрать новый пустой layout:
    val rootDiv = vctx.d.createElement("div")
      .asInstanceOf[HTMLDivElement]
    rootDiv.setAttribute("id", ROOT_ID)
    rootDiv.setAttribute("class", ROOT_CSS_CLASS)
    rootDiv.style.display = "none"

    val layoutDiv = vctx.d.createElement("div")
      .asInstanceOf[HTMLDivElement]
    layoutDiv.setAttribute("id", LAYOUT_ID)

    rootDiv.appendChild(layoutDiv)

    SafeDoc.body.appendChild(rootDiv)

    lctx.rootDiv.set(rootDiv)
    lctx.layoutDiv.set(layoutDiv)
  }


  /** Из-за прозрачностей нужно очистить фон до блеска после приветствия. */
  def eraseBg()(implicit vctx: IVCtx): Unit = {
    def _erase(el: HTMLElement): Unit = {
      el.style.backgroundColor = "#ffffff"
    }
    _erase( SafeDoc.body )
    vctx.layout.rootDiv.get.foreach { el =>
      _erase(el)
    }
  }

  /** Выставить css-класс отображения для layoutDiv. */
  def setWndClass()(implicit vctx: IVCtx): Unit = {
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
      vctx.layout.layoutDiv.get.foreach { layoutDiv =>
        layoutDiv.className = cssClassOrNull
      }
    }
  }

}
