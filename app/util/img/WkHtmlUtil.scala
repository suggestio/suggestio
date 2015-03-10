package util.img

import java.io.File
import java.nio.file.Files

import controllers.routes
import io.suggest.img.ImgCrop
import io.suggest.ym.model.common.{MImgInfoMeta, MImgSizeT}
import models.MAdT
import models.blk.OneAdQsArgs
import play.api.cache.Cache
import util.PlayMacroLogsImpl
import util.async.AsyncUtil
import util.blocks.{BlocksConf, BgImg}
import util.xplay.PlayUtil.httpPort
import models.im._

import scala.concurrent.{Promise, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.14 9:57
 * Description: Утиль для работы с wkhtml2image, позволяющая рендерить html в растровые картинки.
 */

object WkHtmlUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /** Сколько кешировать в памяти сгенеренную картинку. Картинки жирные и нужны недолго, поэтому следует и
    * кеширование снизить до минимума. Это позволит избежать DoS-атаки. */
  val CACHE_TTL_SECONDS = configuration.getInt("wkhtml.cache.ttl.seconds") getOrElse 5

  /**
   * Упрощенная функция для того, чтобы быстро сделать всё круто:
   * - асинхронно
   * - с участием кеша для защиты от DDOS.
   * @param args настройки вызова.
   * @return Фьючерс с прочитанной в память картинкой.
   */
  // TODO Этот метод - эталонный quick and dirty быдлокод. Отрендеренные картинки нужно хранить на винче, не трогая кеш.
  def html2imgSimple(args: WkHtmlArgs): Future[Array[Byte]] = {
    val dstFile = File.createTempFile("wkhtml2image", "." + args.outFmt.name)
    val fut = Future {
      html2img(args, dstFile)
      Files.readAllBytes(dstFile.toPath)
    }(AsyncUtil.singleThreadIoContext)
    fut onComplete { case _ =>
      dstFile.delete()
    }
    fut
  }

  /**
   * Враппер над html2imgSingle() для кеширования результатов.
   * @param args Аргументы для вызова wkhtml2img.
   * @param cacheSec Опциональное время кеширования в секундах.
   * @return Тоже самое, что и нижележащий метод.
   */
  def html2imgSimpleCached(args: WkHtmlArgs, cacheSec: Int = CACHE_TTL_SECONDS): Future[Array[Byte]] = {
    val ck = args.toString
    val p = Promise[Array[Byte]]()
    val pfut = p.future
    val resFut = Cache.getAs [Future[Array[Byte]]] (ck) match {
      case Some(bytesFut) =>
        bytesFut
      case None =>
        Cache.set(ck, pfut, expiration = cacheSec)
        html2imgSimple(args)
    }
    p completeWith resFut
    pfut
  }

  /**
   * Запуск конвертации блокировано.
   * @param args Аргументы вызова.
   * @return Массив байт с картинкой если всё ок.
   *         RuntimeException если вызов wkhtmltoimage вернул не 0.
   *         Exception при иных проблемах.
   */
  def html2img(args: WkHtmlArgsT, dstFile: File): Unit = {
    val cmdargs = args.toCmdLine(List(dstFile.getAbsolutePath))
    val now = System.currentTimeMillis()
    // TODO Асинхронно запускать сие?
    val p = Runtime.getRuntime.exec(cmdargs.toArray)
    val result = p.waitFor()
    val tookMs = System.currentTimeMillis() - now
    lazy val cmd = cmdargs.mkString(" ")
    trace(cmd + "  ===>>>  " + result + " ; took = " + tookMs + "ms")
    if (result != 0) {
      throw new RuntimeException(s"Cannot execute shell command (result: $result) : $cmd")
    }
  }

  /**
   * Генерации абсолютной ссылки на отрендеренную в картинку рекламную карточку.
   * @param adArgs Параметры рендера.
   * @return Строка с абсолютной ссылкой на локалхост.
   */
  def adImgLocalUrl(adArgs: OneAdQsArgs): String = {
    "http://localhost:" + httpPort + routes.MarketShowcase.onlyOneAd(adArgs).url
  }

  /**
   * Рендер указанной рекламной карточки
   * @param adArgs Данные по рендеру.
   * @param mad карточка для рендера.
   * @param fmt Целевой формат.
   * @return Фьючерс с байтами картинки.
   */
  def renderAd2img(adArgs: OneAdQsArgs, mad: MAdT, fmt: OutImgFmt): Future[Array[Byte]] = {
    val sourceAdSz = mad.blockMeta
    // Высота отрендеренной карточки с учетом мультипликатора
    lazy val width0 = (sourceAdSz.width * adArgs.szMult).toInt
    val height = (sourceAdSz.height * adArgs.szMult).toInt
    val fut = adArgs.wideOpt match {
      // Eсли запрошен широкий рендер, то нужно рассчитывать кроп и размер экрана с учётом квантования фоновой картинки.
      case Some(wide) =>
        // Внешняя полная ширина отрендеренной широкой карточки.
        val bc = BlocksConf applyOrDefault mad.blockMeta.blockId
        val wideWidth0 = (wide.width * adArgs.szMult).toInt
        val bgImgInfoOpt = bc.getMadBgImg(mad)
        val cropInfoOptFut = bgImgInfoOpt.fold {
          Future successful Option.empty[ImgCrop]
        } { bgImgInfo =>
          val bgImg = MImg(bgImgInfo.filename)
          val wideWh = MImgInfoMeta(height = height, width = wideWidth0)
          BgImg.getAbsCropOrFail(bgImg, wideWh)
            .map { Some.apply }
        }
        cropInfoOptFut map { cropOpt =>
          cropOpt.fold {
            // Нет предложенного кропа.
            val extWidth = BgImg.normWideWidthBgSz(wideWidth0)
            (extWidth, Option.empty[ImgCrop])
          } { crop =>
            val extWidth = crop.width
            if (extWidth <= wideWidth0) {
              // Предложенный кроп фоновой картинки не превышает запрошенный размер.
              extWidth -> None
            } else /*if (extWidth > wide.width)*/ {
              // Требуется кроп отрендеренной карточки, т.к. предложенный кроп BgImg шире, чем запрошенный размер картинки.
              val cs = Some(ImgCrop(
                width   = wideWidth0,
                height  = height,
                offX    = (extWidth - wideWidth0) / 2,
                offY    = 0
              ))
              extWidth -> cs
            }
          }
        } recover { case ex: NoSuchElementException =>
          (width0, None)
        }

      // Без wide, значит можно рендерить карточку as-is.
      case None =>
        Future successful (width0, None)
    }

    // Запускаем генерацию результата
    fut flatMap { case (extWidth, cropOpt) =>
      val wkArgs = WkHtmlArgs(
        src         = adImgLocalUrl(adArgs),
        scrSz       = MImgInfoMeta(width = extWidth, height = height),
        outFmt      = fmt,
        plugins     = false,
        crop        = cropOpt
      )
      WkHtmlUtil.html2imgSimpleCached(wkArgs)
    }
  }

}

