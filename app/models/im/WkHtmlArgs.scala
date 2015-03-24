package models.im

import java.io.File

import models.{ImgCrop, MImgSizeT}
import play.api.Play.{current, configuration}
import util.PlayMacroLogsDyn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:43
 * Description: Модель параметров для вызова wkhtml2image.
 */

object WkHtmlArgs extends PlayMacroLogsDyn with IAdRendererCompanion {

  /** Название/путь к утили, вызываемой из командной строки. */
  val WKHTML2IMG = configuration.getString("wkhtml.toimg.prog.name") getOrElse "wkhtmltoimage"

  /** path для директории кеширования. */
  val CACHE_DIR: Option[String] = {
    val dirPath = configuration.getString("wkhtml.cache.dir") getOrElse "/tmp/sio2/wkhtml/cache"
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

  override def forArgs(src: String, scrSz: MImgSizeT, quality: Option[Int], outFmt: OutImgFmt): IAdRenderArgs = {
    apply(src = src, scrSz = scrSz, quality = quality, outFmt = outFmt)
  }
}


import WkHtmlArgs._


/** Абстрактные аргументы для вызова wkhtml2image. */
trait WkHtmlArgsT extends IAdRenderArgsSyncFile with PlayMacroLogsDyn {

  /** Отмасштабировать страницу? */
  def zoomOpt : Option[Float]

  /** Разрешить браузерные плагины? */
  def plugins : Boolean

  /** Необязательный кроп. */
  def crop    : Option[ImgCrop]

  /** Разрешать ли wkhtml переопределять заданную ширину? Нужно patched Qt version installed. */
  def smartWidth: Boolean

  /** Сборка строки вызова wkhtml2image. */
  def toCmdLineArgs(acc0: List[String] = Nil): List[String] = {
    var l =
      "--width" :: scrSz.width.toString ::
      "--height" :: scrSz.height.toString ::
      src ::
      acc0
    if (quality.isDefined)
      l = "--quality" :: quality.get.toString :: l
    if (zoomOpt.isDefined)
      l = "--zoom" :: zoomOpt.get.toString :: l
    if (!plugins)
      l ::= "--disable-plugins"
    if (CACHE_DIR.isDefined)
      l = "--cache-dir" :: CACHE_DIR.get :: l
    if (crop.isDefined) {
      val c = crop.get
      l = "--crop-x" :: c.offX.toString ::
        "--crop-y" :: c.offY.toString ::
        "--crop-w" :: c.width.toString ::
        "--crop-h" :: c.height.toString ::
        l
    }
    if (!smartWidth)
      l ::= "--disable-smart-width"
    l
  }

  /** Название проги wkhtml2image и аргументы. Можно сразу отправлять на исполнение. */
  def toCmdLine(acc0: List[String] = Nil): List[String] = {
    WKHTML2IMG :: toCmdLineArgs(acc0)
  }


  /** Синхронный рендер с помощью WkHtml2image. */
  override def renderSync(dstFile: File): Unit = {
    val cmdargs = toCmdLine(List(dstFile.getAbsolutePath))
    exec(cmdargs.toArray)
  }
}


/** Реализация аргументов для вызова wkhtml. */
case class WkHtmlArgs(
  src         : String,
  scrSz       : MImgSizeT,
  outFmt      : OutImgFmt         = AdRenderArgs.OUT_FMT_DFLT,
  quality     : Option[Int]       = None,
  zoomOpt     : Option[Float]     = None,
  plugins     : Boolean           = false,
  crop        : Option[ImgCrop]   = None,
  smartWidth  : Boolean           = true
) extends WkHtmlArgsT

