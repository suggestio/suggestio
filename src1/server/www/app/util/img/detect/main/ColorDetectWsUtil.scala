package util.img.detect.main

import javax.inject.{Inject, Singleton}

import io.suggest.playx.CacheApiUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.{MHistogram, MAnyImgT}
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
                                   cacheApiUtil                : CacheApiUtil,
                                   wsDispatcherActors          : WsDispatcherActors,
                                   implicit private val ec     : ExecutionContext
                                 )
  extends MacroLogsImpl
{

  /** Размер генерируемой палитры. */
  private def MAIN_COLORS_PALETTE_SIZE = 8

  /** Размер возвращаемой по WebSocket палитры. */
  private def MAIN_COLORS_PALETTE_SHRINK_SIZE = 4


  /**
   * Запуск в фоне определения палитры и отправки уведомления по веб-сокету.
   *
   * @param im Картинка для обработки.
   * @param wsId id для уведомления.
   */
  // TODO Это compat-функция для старых карточек и старой схемы аплоада. Просто дёргает три разрые функции. Удалить её вместе со старым хламом.
  def detectPalletteToWs(im: MAnyImgT, wsId: String): Future[MHistogram] = {
    shrinkedToWs(wsId) {
      mainColorDetector.cached(im) {
        mainColorDetector.detectPaletteFor(im, maxColors = MAIN_COLORS_PALETTE_SIZE)
      }
    }
  }


  def shrinkedToWs(wsId: String)(fut: Future[MHistogram]): Future[MHistogram] = {
    for (mhist0 <- fut) yield {
      val mhist2 = if (MAIN_COLORS_PALETTE_SHRINK_SIZE < MAIN_COLORS_PALETTE_SIZE) {
        mhist0.copy(
          sorted = mhist0.sorted.take(MAIN_COLORS_PALETTE_SHRINK_SIZE)
        )
      } else {
        mhist0
      }
      wsDispatcherActors.notifyWs(wsId, mhist2)
      mhist2
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[ColorDetectWsUtil]]. */
trait IColorDetectWsUtilDi {
  def colorDetectWsUtil: ColorDetectWsUtil
}
