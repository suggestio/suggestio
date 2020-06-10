package io.suggest.dev

import io.suggest.common.geom.d2.{MOrientations2d, MSize2di}
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.common.vsz.ViewportSz
import japgolly.univeq._

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


  /** Задетектить небезопасные для контента области на экране.
    * iphone10 содержит вырез наверху экрана.
    *
    * @param mscreen Результат getScreen().
    * @return Данные боковин экрана.
    */
  def getScreenUnsafeAreas(mscreen: MScreen): MTlbr = {
    try {
      val uaOpt = WindowVm()
        .navigator
        .flatMap(_.userAgent)

      def orientation = MOrientations2d.forSize2d( mscreen.wh )

      // Const: Отступ сверху на 12px
      def TOP_12PX = MTlbr( topO = Some(12) )

      if ( uaOpt.exists(_ contains "iPhone") ) {
        val screenWhs = mscreen.wh.width ::
          mscreen.wh.height ::
          Nil

        // Это айфон. Надо решить, сколько отсутупать.
        if (
          // iPhone 10 | iPhone 11 Pro
          (screenWhs contains 812) &&
          (screenWhs contains 375) &&
          (mscreen.pxRatio ==* MPxRatios.DPR3)
        ) {
          // TODO Определять как-то автоматически? Можно рендерить с css-свойствами и мерять координаты, затем накидывать смещения как-то.
          MTlbr(
            topO  = Option.when(orientation ==* MOrientations2d.Vertical)( 28 ),
            leftO = Option.when(orientation ==* MOrientations2d.Horizontal)( 36 ),
            // TODO right или left? Надо как-то врубаться, куда ориентация направлена. Можно детектить через доп. css-свойства apple.
            bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
          )

        } else if (
          // iPhone 11 @2 / 11 Pro Max @3 (2019)
          (screenWhs contains 896) &&
          (screenWhs contains 414)
        ) {
          mscreen.pxRatio match {
            case MPxRatios.DPR3 =>
              // TODO Отладить значения
              MTlbr(
                topO  = Option.when( orientation ==* MOrientations2d.Vertical )( 32 ),
                leftO = Option.when( orientation ==* MOrientations2d.Horizontal )( 36 ),
                bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
              )
            case _ =>
              // TODO Отладить значения
              MTlbr(
                topO  = Option.when( orientation ==* MOrientations2d.Vertical )( 32 ),
                leftO = Option.when( orientation ==* MOrientations2d.Horizontal )( 36 ),
                bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
              )
          }

        } else {
          // На остальных айфонах надо делать 12px сверху в вертикальной ориентации.
          if (orientation ==* MOrientations2d.Vertical)
            TOP_12PX
          else
            MTlbr.empty
        }

      } else if ( uaOpt.exists(_ contains "iPad") ) {
        // 12px сверху в любой ориентации
        TOP_12PX

      } else {
        // Обычное устройство, всё ок.
        MTlbr.empty
      }

    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.SCREEN_SAFE_AREA_DETECT_ERROR, ex, msg = mscreen )
        MTlbr.empty
    }
  }

}
