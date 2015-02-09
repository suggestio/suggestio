package models.im

import java.io.File

import models.MImgSizeT
import play.api.Play.{current, configuration}
import util.PlayMacroLogsDyn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:43
 * Description: Модель параметров для вызова wkhtml2image.
 */

object WkHtmlArgs extends PlayMacroLogsDyn {

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

  /** Дефолтовый формат сохраняемой картинки. */
  def outFmtDflt = OutImgFmts.PNG

}


import WkHtmlArgs._


/** Абстрактные аргументы для вызова wkhtml2image. */
trait WkHtmlArgsT {

  def src     : String
  def imgSize : MImgSizeT
  def quality : Option[Int]
  def outFmt  : OutImgFmt
  def zoomOpt : Option[Float]
  def plugins : Boolean

  /** Сборка строки вызова wkhtml2image. */
  def toCmdLineArgs(acc0: List[String] = Nil): List[String] = {
    var l =
      "--width" :: imgSize.width.toString ::
      "--height" :: imgSize.height.toString ::
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
    l
  }

  /** Название проги wkhtml2image и аргументы. Можно сразу отправлять на исполнение. */
  def toCmdLine(acc0: List[String] = Nil): List[String] = WKHTML2IMG :: toCmdLineArgs(acc0)

}


/** Реализация аргументов для вызова wkhtml. */
case class WkHtmlArgs(
  src     : String,
  imgSize : MImgSizeT,
  outFmt  : OutImgFmt = WkHtmlArgs.outFmtDflt,
  quality : Option[Int] = None,
  zoomOpt : Option[Float] = None,
  plugins : Boolean = false
) extends WkHtmlArgsT

