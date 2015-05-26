package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mgrid.MGrid
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.grid.GridView
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:22
 * Description: Контроллер сетки.
 */
object GridCtl extends CtlT with SjsLogger {

  /**
   * Посчитать и сохранить новые размеры сетки для текущих параметров оной.
   * Обычно этот метод вызывается в ходе добавления карточек в плитку.
   */
  def resetContainerSz(): Unit = {
    // Вычислить размер.
    val sz = MGrid.getContainerSz()
    // Обновить модель сетки новыми данными, и view-контейнеры.
    GridView.setContainerSz(sz)
    MGrid.updateState(sz)
  }

  /** GridView решил, что нужно загрузить ещё карточек в view. */
  def needToLoadMoreAds(): Unit = {
    ???
  }

  /**
   * От сервера получена новая пачка карточек для выдачи.
   * @param resp ответ сервера.
   */
  def newAdsReceived(resp: MFindAds): Unit = {
    warn("TODO: " + resp)
    ???
  }

}
