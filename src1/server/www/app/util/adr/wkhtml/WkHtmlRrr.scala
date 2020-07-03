package util.adr.wkhtml

import java.io.File

import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Singleton}

import io.suggest.async.AsyncUtil
import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.{MImgFormat, MImgFormats}
import io.suggest.util.logs.MacroLogsDyn
import models.adr._
import models.mproj.ICommonDi
import play.api.Configuration
import util.adr.{IAdRrr, IAdRrrDiFactory, IAdRrrUtil}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.16 18:21
  * Description: Поддержка рендеринга карточек через wkhtmltoimage.
  */
@Singleton
class WkHtmlRrrUtil @Inject() (
  configuration: Configuration
)
  extends IAdRrrUtil
  with MacroLogsDyn
{

  /** Название/путь к утили, вызываемой из командной строки. */
  val WKHTML2IMG = configuration.getOptional[String]("wkhtml.toimg.prog.name").getOrElse("wkhtmltoimage")

  /** path для директории кеширования. */
  val CACHE_DIR: Option[String] = {
    val dirPath = configuration.getOptional[String]("wkhtml.cache.dir")
      .getOrElse("/tmp/sio2/wkhtml/cache")
    val dirFile = new File(dirPath)
    val logger = LOGGER
    if ((dirFile.exists && dirFile.isDirectory)  ||  dirFile.mkdirs) {
      logger.debug("WkHtml cache dir is set to " + dirPath)
      Some(dirPath)
    } else {
      logger.warn("WkHtml cache dir in not created/unset or invalid. Working without cache.")
      None
    }
  }

  /** Дефолтовое значение quality, если не задано. */
  override def qualityDflt(scrSz: ISize2di, fmt: MImgFormat): Option[Int] = {
    fmt match {
      // Для усиления сжатия PNG нужно выставлять "--quality 0" для wkhtmltoimage. Это уменьшает размер в несколько раз.
      case MImgFormats.PNG => Some(0)
      case _              => super.qualityDflt(scrSz, fmt)
    }
  }

}


/** Интерфейс Guice DI-factory инстансов [[WkHtmlRrr]]. */
trait WkHtmlRrrDiFactory extends IAdRrrDiFactory {
  override def instance(args: IAdRenderArgs): WkHtmlRrr
}


/** Рендерер для wkhtml для указанных параметров.. */
class WkHtmlRrr @Inject() (
  @Assisted override val args   : IAdRenderArgs,
  override val asyncUtil        : AsyncUtil,
  util                          : WkHtmlRrrUtil,
  override val mCommonDi        : ICommonDi
)
  extends IAdRrr
{

  /** Сборка строки вызова wkhtml2image. */
  private def toCmdLineArgs(acc0: List[String] = Nil): List[String] = {
    val args1: IWkHtmlArgsT = args match {
      case argsWk: MWkHtmlArgs =>
        argsWk
      case simple =>
        new IWkHtmlArgsT with IAdRenderArgsWrapper with IWhHtmlArgsDflt {
          override def _underlying = simple
        }
    }

    var l =
      "--width"  :: args1.scrSz.width.toString ::
      "--height" :: args1.scrSz.height.toString ::
      args1.src ::
      acc0
    for (q <- args1.quality)
      l = "--quality" :: q.toString :: l
    for (zoom <- args1.zoomOpt)
      l = "--zoom" :: zoom.toString :: l
    if (!args1.plugins)
      l ::= "--disable-plugins"

    for (cacheDir <- util.CACHE_DIR)
      l = "--cache-dir" :: cacheDir :: l

    for (c <- args1.crop) {
      l = "--crop-x" :: c.offX.toString ::
        "--crop-y" :: c.offY.toString ::
        "--crop-w" :: c.width.toString ::
        "--crop-h" :: c.height.toString ::
        l
    }
    if (!args1.smartWidth)
      l ::= "--disable-smart-width"
    l
  }

  /** Название проги wkhtml2image и аргументы. Можно сразу отправлять на исполнение. */
  private def toCmdLine(acc0: List[String] = Nil): List[String] = {
    util.WKHTML2IMG :: toCmdLineArgs(acc0)
  }


  override def isFinishedOk(returnCode: Int): Boolean = {
    // TODO 2016.jan.18 Почему-то на wkhtmltoimage-xvfb серверах возвращает 1, хотя судя по stdout всё ок.
    returnCode == 1 || super.isFinishedOk(returnCode)
  }

  /** Синхронный рендер с помощью WkHtml2image. */
  override def renderSync(dstFile: File): Unit = {
    val cmdargs = toCmdLine(List(dstFile.getAbsolutePath))
    exec(cmdargs.toArray)
  }

}
