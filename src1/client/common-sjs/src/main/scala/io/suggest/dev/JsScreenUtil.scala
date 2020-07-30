package io.suggest.dev

import io.suggest.common.geom.d2.MSize2di
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
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
      logger.warn( ErrorMsgs.NO_SCREEN_VSZ_DETECTED )

    val pxRatio = WindowVm()
      .devicePixelRatio
      .fold {
        logger.warn( ErrorMsgs.SCREEN_PX_RATIO_MISSING )
        MPxRatios.default
      }( MPxRatios.forRatio(_) )

    vszOpt.fold{
      // Наврядли этот код будет вызываться когда-либо.
      MScreen.pxRatio.set( pxRatio )(MScreen.default)
    } { sz2d =>
      MScreen(
        wh = MSize2di(
          width  = sz2d.width,
          height = sz2d.height,
        ),
        pxRatio = pxRatio
      )
    }
  }

}
