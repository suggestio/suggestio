package io.suggest.sc.sjs.v.render.direct.index

import io.suggest.sc.sjs.m.IAppState
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.v.render.IRenderer
import io.suggest.sc.ScConstants.Layout._
import io.suggest.sc.sjs.v.render.direct.m.ReDrawLayoutResult
import io.suggest.sc.sjs.v.render.direct.res.RendererResT
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:14
 * Description: Аддон для поддержки отображения полученного от сервера showcase index.
 */
trait ShowIndex extends IRenderer with RendererResT {

  /**
   * Перерисовать layout выдачи.
   * @param state Состояние приложения.
   */
  private def reDrawLayout()(implicit state: IAppState): ReDrawLayoutResult = {
    val d = dom.document

    // Вычистить предыдущуй layout и его ресурсы:
    commonRes.recreate()
    focusedRes.recreate()

    val rootId = ROOT_ID

    // Удалить старый root div.
    val oldRoot = d.getElementById(rootId)
    if (oldRoot != null) {
      oldRoot.parentNode
        .removeChild(oldRoot)
    }

    // TODO sm.geo.active_layer = null   // Выставить текущий выбранный слой в колонке навигации по узлам в состоянии.

    // Собрать новый пустой layout:

    val layoutDiv = d.createElement("div")
      .asInstanceOf[HTMLDivElement]
    layoutDiv.setAttribute("id", LAYOUT_ID)

    val rootDiv = d.createElement("div")
      .asInstanceOf[HTMLDivElement]
    rootDiv.setAttribute("id", rootId)
    rootDiv.setAttribute("class", ROOT_CSS_CLASS)
    rootDiv.appendChild(layoutDiv)
    rootDiv.style.display = "none"

    state.safeDoc.body.appendChild(rootDiv)

    ReDrawLayoutResult(
      rootDiv   = rootDiv,
      layoutDiv = layoutDiv
    )
  }


  /**
   * Получены новые данные node index. Нужно стереть старый layout, впилить новый.
   * @param minx Данные MNodeIndex от сервера.
   * @param state Доступ к состоянию приложения.
   * @return void, когда всё закончится.
   */
  override def showIndex(minx: MNodeIndex)(implicit state: IAppState): Unit = {
    val rdRes = reDrawLayout()
    // TODO bind_window_events() - реагировать на ресайз. Но это наверное должно происходить уровнем выше.
    val wnd = dom.window
    wnd.scrollTo(0, 0)

    rdRes.rootDiv.style.display = "block"
    rdRes.layoutDiv.innerHTML = minx.html
    state.safeDoc.body.style.overflow = "hidden"

    ???
  }

}
