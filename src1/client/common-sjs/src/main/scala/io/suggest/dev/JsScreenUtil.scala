package io.suggest.dev

import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.{MOrientations2d, MSize2di}
import io.suggest.msg.ErrorMsgs
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
      LOG.warn( ErrorMsgs.NO_SCREEN_VSZ_DETECTED )

    val pxRatio = WindowVm()
      .devicePixelRatio
      .fold {
        LOG.warn( ErrorMsgs.SCREEN_PX_RATIO_MISSING )
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


  /** Задетектить небезопасные для контента области на экране.
    * iphone10 содержит вырез наверху экрана.
    *
    * @param mscreen Результат getScreen().
    * @return Данные боковин экрана.
    */
  def getScreenUnsafeAreas(mscreen: MScreen): MTlbr = {
    try {
      val ua = dom.window.navigator.userAgent
      def orientation = MOrientations2d.forSize2d( mscreen.wh )

      // Const: Отступ сверху на 12px
      def TOP_12PX = MTlbr( topO = Some(12) )

      if ( ua contains "iPhone" ) {
        // Это айфон. Надо решить, сколько отсутупать.
        val isIphone10Wh = {
          val wh = mscreen.wh.width ::
            mscreen.wh.height ::
            Nil

          (wh contains 812) &&
            (wh contains 375) &&
            (mscreen.pxRatio ==* MPxRatios.DPR3)
        }

        if (isIphone10Wh) {
          // TODO Определять как-то автоматически? Можно рендерить с css-свойствами и мерять координаты, затем накидывать смещения как-то.
          MTlbr(
            topO  = OptionUtil.maybe(orientation ==* MOrientations2d.Vertical)( 28 ),
            leftO = OptionUtil.maybe(orientation ==* MOrientations2d.Horizontal)( 36 )
            // TODO right или left? Надо как-то врубаться, куда ориентация направлена. Можно детектить через доп. css-свойства apple.
          )
        } else {
          // На остальных айфонах надо делать 12px сверху в вертикальной ориентации.
          if (orientation ==* MOrientations2d.Vertical)
            TOP_12PX
          else
            MTlbr.empty
        }

      } else if ( ua contains "iPad" ) {
        // 12px сверху в любой ориентации
        TOP_12PX

      } else {
        // Обычное устройство, всё ок.
        MTlbr.empty
      }

    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.SCREEN_SAFE_AREA_DETECT_ERROR, ex, msg = mscreen )
        MTlbr.empty
    }
  }

}
