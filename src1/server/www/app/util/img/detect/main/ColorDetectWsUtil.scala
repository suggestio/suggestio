package util.img.detect.main

import javax.inject.{Inject, Singleton}

import io.suggest.ad.form.AdFormConstants
import io.suggest.color.MHistogram
import io.suggest.util.logs.MacroLogsImpl
import models.im.MAnyImgT
import util.ws.WsDispatcherActors

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 14:49
  * Description: Утиль для простого связывания MainColorDetector и WebSockets.
  */
@Singleton
class ColorDetectWsUtil @Inject()(
                                   mainColorDetector           : MainColorDetector,
                                   wsDispatcherActors          : WsDispatcherActors,
                                   implicit private val ec     : ExecutionContext
                                 )
  extends MacroLogsImpl
{


  /**
   * Запуск в фоне определения палитры и отправки уведомления по веб-сокету.
   *
   * @param im Картинка для обработки.
   * @param wsId id для уведомления.
   */
  // TODO Это compat-функция для старых карточек и старой схемы аплоада. Просто дёргает три разрые функции. Удалить её вместе со старым хламом.
  def detectPalletteToWs(im: MAnyImgT, wsId: String): Future[MHistogram] = {
    shrinked4adEditor(wsId) {
      mainColorDetector.cached(im) {
        mainColorDetector.detectPaletteFor(im, maxColors = AdFormConstants.ColorDetect.PALETTE_SIZE)
      }
    }
  }


  def shrinked4adEditor(wsId: String)(fut: Future[MHistogram]): Future[MHistogram] = {
    for (mhist0 <- fut) yield {
      val mhist2 = mhist0.shrinkColorsCount( AdFormConstants.ColorDetect.PALETTE_SHRINK_SIZE )
      wsDispatcherActors.notifyWs(wsId, mhist2)
      mhist2
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[ColorDetectWsUtil]]. */
trait IColorDetectWsUtilDi {
  def colorDetectWsUtil: ColorDetectWsUtil
}
