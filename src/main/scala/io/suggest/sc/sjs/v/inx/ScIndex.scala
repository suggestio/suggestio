package io.suggest.sc.sjs.v.inx

import io.suggest.sc.sjs.m.SafeDoc
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.grid.GridView

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:14
 * Description: Управление представлением showcase index.
 */
object ScIndex {

  /**
   * Получены новые данные node index. Нужно стереть старый layout, впилить новый.
   * @param minx Данные MNodeIndex от сервера.
   * @return void, когда всё закончится.
   */
  def showIndex(minx: MNodeIndex)(implicit vctx: IVCtx): Unit = {
    // TODO bind_window_events() - реагировать на ресайз. Но это наверное должно происходить уровнем выше.
    val wnd = vctx.w
    wnd.scrollTo(0, 0)

    val lctx = vctx.layout
    lctx.rootDiv().style.display = "block"
    lctx.layoutDiv().innerHTML = minx.html
    SafeDoc.body.style.overflow = "hidden"
  }

}
