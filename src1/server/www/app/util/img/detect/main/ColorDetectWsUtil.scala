package util.img.detect.main

import javax.inject.{Inject, Singleton}

import io.suggest.playx.CacheApiUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.{Histogram, MAnyImgT, MImgT}
import util.ws.WsDispatcherActors

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

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

  /** Настройка кеширования для  */
  protected def CACHE_COLOR_HISTOGRAM_SEC = 10

  /**
   * Запуск в фоне определения палитры и отправки уведомления по веб-сокету.
   *
   * @param im Картинка для обработки.
   * @param wsId id для уведомления.
   */
  def detectPalletteToWs(im: MAnyImgT, wsId: String): Future[Histogram] = {
    // Кеширование ресурсоемких результатов работы MCD.
    val f = { () =>
      mainColorDetector.detectPaletteFor(im, maxColors = MAIN_COLORS_PALETTE_SIZE)
    }
    val cacheSec = CACHE_COLOR_HISTOGRAM_SEC
    val fut = if (cacheSec > 0) {
      cacheApiUtil.getOrElseFut("mcd." + im.rowKeyStr + ".hist", cacheSec.seconds)(f())
    } else {
      f()
    }
    fut.andThen {
      case Success(result) =>
        val res2 = if (MAIN_COLORS_PALETTE_SHRINK_SIZE < MAIN_COLORS_PALETTE_SIZE) {
          result.copy(
            sorted = result.sorted.take(MAIN_COLORS_PALETTE_SHRINK_SIZE)
          )
        } else {
          result
        }
        wsDispatcherActors.notifyWs(wsId, res2)
      case Failure(ex) =>
        LOGGER.warn("Failed to execute color detector on tmp img " + im.fileName, ex)
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[ColorDetectWsUtil]]. */
trait IColorDetectWsUtilDi {
  def colorDetectWsUtil: ColorDetectWsUtil
}
