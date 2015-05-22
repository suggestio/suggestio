package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mgrid.MGrid
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.grid.GridView

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:22
 * Description: Контроллер сетки.
 */
object GridCtl extends CtlT {

  /**
   * Посчитать и сохранить новые размеры сетки для текущих параметров оной.
   * Обычно этот метод вызывается в ходе действий другого контроллера.
   * @param vcxt Контекст рендера шаблонов, если есть.
   */
  def resetContainerSz()(implicit vcxt: IVCtx = vctx): Unit = {
    // Вычислить размер.
    val sz = MGrid.getContainerSz()
    // Обновить модель сетки новыми данными, и view-контейнеры.
    GridView.setContainerSz(sz)(vctx)
    MGrid.updateState(sz)
  }


  /** GridView понял, что нужно загрузить ещё карточек в view. */
  def loadMoreAds(): Unit = {
    ???
  }

}
