package io.suggest.dev

import cordova.plugins.device.{CordovaPluginDevice, CordovaPluginDeviceUtil}
import io.suggest.common.geom.d2.MOrientations2d
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.JsScreenUtil.logger
import io.suggest.msg.ErrorMsgs
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.07.2020 12:19
  * Description: Утиль для работы с реальными экранами, которые не всегда прямоугольные и
  * не всегда имеют геометрически-правильную форму.
  */
object HwScreenUtil {

  // Const: Отступ сверху на 12px
  private def _TOP_12PX = MTlbr( topO = Some(14) )

  /** Задетектить небезопасные для контента области на экране.
    * iphone10 содержит вырез наверху экрана.
    * Вызывать можно только после наступления cordova platform ready.
    *
    * @param mscreen Результат getScreen().
    * @return Данные боковин экрана.
    */
  def getScreenUnsafeAreas(mscreen: MScreen): MTlbr = {
    try {
      Option.when(
        CordovaConstants.isCordovaPlatform() &&
        // Бывает, что device-плагин ещё не инициализировался.
        CordovaPluginDeviceUtil.isAvailable()
      ) {
        // С user-agent'ами полный швах и зоопарк, даже внутри cordova:
        // Например, iPad любит прикидываться компом.
        // Поэтому пытаемся получить данные через cordova-plugin-device:
        val platform = CordovaPluginDevice.platform

        if (platform equalsIgnoreCase CordovaPluginDeviceUtil.Platform.Android) {
          // ~25 csspx - размер статусбара, который всегда сверху при любом повороте экрана.
          MTlbr(
            topO = Some( 26 ),
          )

        } else if (platform equalsIgnoreCase CordovaPluginDeviceUtil.Platform.iOS) {
          // Для iOS возможны варианты.
          val devModel = CordovaPluginDevice.model
          val orientation = MOrientations2d.forSize2d( mscreen.wh )

          if (devModel startsWith "iPhone13,") {
            // iphone 12 - вырез наверху отличается от предыдущих поколений.
            MTlbr(
              topO  = Option.when(orientation ==* MOrientations2d.Vertical)( 36 ),
              leftO = Option.when(orientation ==* MOrientations2d.Horizontal)( 40 ),
              bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
            )

          } else if (devModel startsWith "iPhone") {
            // Это айфон. С model id всё необычно:
            // iPhone12,5 = 11 pro max
            //       12,3 = 11 pro
            //       12,1 = 11
            // Несмотря на отличие в одну цифирку, экраны сильно различаются.
            // Наборы свойств хардварных экранов повторяются между поколениями, при этом очень различаются внутри поколения.
            // Поэтому игнорим model id, и матчим по фактическому размеру экрана.
            val screenWhs = Set.empty[Int] + mscreen.wh.width + mscreen.wh.height

            if (
              // iPhone 10 | iPhone 11 Pro
              (screenWhs contains 812) &&
              (screenWhs contains 375) &&
              (mscreen.pxRatio ==* MPxRatios.DPR3)
            ) {
              // TODO Определять как-то автоматически? Можно рендерить с css-свойствами и мерять координаты, затем накидывать смещения как-то.
              MTlbr(
                topO  = Option.when(orientation ==* MOrientations2d.Vertical)( 32 ),
                leftO = Option.when(orientation ==* MOrientations2d.Horizontal)( 36 ),
                // TODO right или left? Надо как-то врубаться, куда ориентация направлена. Можно детектить через доп. css-свойства apple.
                bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
              )

            } else if (
              // iPhone 11 @2 / 11 Pro Max @3 (2019)
              (screenWhs contains 896) &&
              (screenWhs contains 414)
            ) {
              MTlbr(
                topO  = Option.when( orientation ==* MOrientations2d.Vertical )( 32 ),
                leftO = Option.when( orientation ==* MOrientations2d.Horizontal )( 32 ),
                bottomO = Option.when( orientation ==* MOrientations2d.Vertical )( 12 ),
              )
            } else {
              // На остальных айфонах надо делать 12px сверху в вертикальной ориентации.
              //println( "iphone screen model unknown" )
              if (orientation ==* MOrientations2d.Vertical) {
                _TOP_12PX
              } else {
                //println( "iphone unknown horizontal" )
                MTlbr.empty
              }
            }


          } else if (devModel startsWith "iPad") {
            // Это ipad
            _TOP_12PX
          } else {
            // Не ясно что это.
            logger.warn( ErrorMsgs.SCREEN_SAFE_AREA_DETECT_ERROR, msg = (mscreen, platform, devModel) )
            MTlbr.empty
          }

        } else {
          //println("platform unsupported: " + platform + " model=" + CordovaPluginDevice.model)
          MTlbr.empty
        }
      }
      .getOrElse {
        //println("no cordova found")
        MTlbr.empty
      }

    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.SCREEN_SAFE_AREA_DETECT_ERROR, ex, msg = mscreen )
        MTlbr.empty
    }
  }

}
