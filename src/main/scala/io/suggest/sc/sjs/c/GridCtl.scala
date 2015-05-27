package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mgrid.MGrid
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.v.grid.GridView
import io.suggest.sc.sjs.v.res.CommonRes
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
    if (resp.mads.isEmpty) {
      log("No more ads")
      ??? // TODO

    } else {
      // Если получены новые параметры сетки, то выставить их в состояние сетки
      for {
        params2   <- resp.params
        newParams <- MGrid.params.importIfChangedFrom(params2)
      } {
        // TODO Нужно спиливать карточки, очищая сетку, если в ней уже есть какие-то карточки, отрендеренные
        // на предыдущих параметрах.
        MGrid.params = newParams
      }

      // Закачать в выдачу новый css.
      resp.css.foreach { css =>
        CommonRes.appendCss(css)
      }

      // TODO Отобразить все новые карточки на экране.
      ???
    }
  }

}
