package io.suggest.dev

import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MOrientations2d
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.common.vsz.ViewportSz
import japgolly.univeq._
import org.scalajs.dom

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
      .fold {
        LOG.warn( WarnMsgs.SCREEN_PX_RATIO_MISSING )
        MPxRatios.default
      }( MPxRatios.forRatio(_) )

    vszOpt.fold{
      // Наврядли этот код будет вызываться когда-либо.
      MScreen.default
        .withPxRatio( pxRatio )
    } { sz2d =>
      MScreen(
        width  = sz2d.width,
        height = sz2d.height,
        pxRatio = pxRatio
      )
    }
  }


  /** Задетектить небезопасные для контента области на экране.
    * iphone10 содержит вырез наверху экрана.
    *
    * @param mscreen Результат getScreen().
    * @param platform Данные по текущей платформе.
    * @return Данные боковин экрана.
    */
  def getScreenUnsafeAreas(mscreen: MScreen, platform: MPlatformS): MTlbr = {
    try {
      val ua = dom.window.navigator.userAgent
      val isIphone = ua.contains("iPhone")
      val isIphone10Wh = isIphone && {
        val wh = mscreen.width ::
          mscreen.height ::
          Nil

        (wh contains 812) &&
          (wh contains 375) &&
          (mscreen.pxRatio ==* MPxRatios.DPR3)
      }

      if (isIphone10Wh) {
        val orientation = MOrientations2d.forSize2d( mscreen )
        // TODO Определять динамически. Для iphone 10 надо 20px, но визуально надо больше.
        val offsetPx = 28
        MTlbr(
          topO  = OptionUtil.maybe(orientation ==* MOrientations2d.Vertical)( offsetPx ),
          leftO = OptionUtil.maybe(orientation ==* MOrientations2d.Horizontal)( offsetPx )
          // TODO right или left? Надо как-то врубаться, куда ориентация направлена. Можно детектить через доп. css-свойства apple.
        )
      } else {
        // Обычное устройство, всё ок.
        MTlbr.empty
      }
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.SCREEN_SAFE_AREA_DETECT_ERROR, ex, msg = (mscreen, platform) )
        MTlbr.empty
    }
  }

}
