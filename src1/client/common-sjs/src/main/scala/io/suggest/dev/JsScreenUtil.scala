package io.suggest.dev

import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.common.vsz.ViewportSz

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 15:47
  * Description: Js-only утиль для работами с экранами устройств.
  */
object JsScreenUtil extends Log {

  /** Детектор данных по экрану. */
  def getScreen(): MScreen = {
    val vszOpt = ViewportSz.getViewportSize()
    if (vszOpt.isEmpty)
      LOG.warn( WarnMsgs.NO_SCREEN_VSZ_DETECTED )

    val pxRatio = WindowVm()
      .devicePixelRatio
      .fold[Double] {
        LOG.warn( WarnMsgs.SCREEN_PX_RATIO_MISSING )
        1.0
      }( MScreen.roundPxRatio )

    vszOpt.fold{
      // Наврядли этот код будет вызываться когда-либо.
      MScreen(
        width  = 1024,
        height = 768,
        pxRatio = pxRatio
      )
    } { sz2d =>
      MScreen(
        width  = sz2d.width,
        height = sz2d.height,
        pxRatio = pxRatio
      )
    }
  }

}
