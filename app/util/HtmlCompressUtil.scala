package util

import java.io.File
import java.nio.file.Files

import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import io.suggest.playx.IsAppModes
import play.api.libs.json.JsString
import play.api.mvc.Filter
import play.api.{Configuration, Environment}
import play.twirl.api.{Html, Txt}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 10:18
 * Description: Утиль для сжатия HTML-ответов.
 */
@Singleton
class HtmlCompressUtil @Inject() (configuration: Configuration, env: Environment)
  extends IsAppModes
{ outer =>

  override protected def appMode = env.mode

  val PRESERVE_LINE_BREAKS_DFLT   = getBool("html.compress.global.preserve.line.breaks", isDev)
  val REMOVE_COMMENTS_DFLT        = getBool("html.compress.global.remove.comments", isProd)
  val REMOVE_INTERTAG_SPACES_DFLT = getBool("html.compress.global.remove.spaces.intertag", true)
  val STRIP_HTTP_PROTO            = getBool("html.compress.global.remove.proto.http", false)
  val STRIP_HTTPS_PROTO           = getBool("html.compress.global.remove.proto.https", false)

  def getForGlobalUsing = {
    val compressor = new HtmlCompressor()
    compressor.setPreserveLineBreaks(PRESERVE_LINE_BREAKS_DFLT)
    compressor.setRemoveComments(REMOVE_COMMENTS_DFLT)
    compressor.setRemoveIntertagSpaces(REMOVE_INTERTAG_SPACES_DFLT)
    // ВООБЩЕ НЕЛЬЗЯ удалять http: в начале URL, т.к. бывает дерганье HTML с левых мест (cordova-приложение, например),
    // а этот компрессор неудачно стрипает протокол в зависимости от ситуации.
    compressor.setRemoveHttpProtocol(false)
    compressor.setRemoveHttpsProtocol(false)
    compressor
  }

  /** Прочитать булево значение из конфига через строку, которая может быть не только true/false. */
  private def getBool(confKey: String, dflt: Boolean): Boolean = {
    val result = configuration.getString(confKey).map {
      _.toLowerCase match {
        case "true"  | "1" | "+" | "yes" | "on" => true
        case "false" | "0" | "-" | "no" | "off" => false
        case "isprod" | "is_prod"               => isProd
        case "isdev" | "is_dev"                 => isDev
      }
    } getOrElse {
      dflt
    }
    result
  }


  /** Перманентный компрессор для html-in-json, который активно используется в выдаче. */
  private val html4jsonCompressor = {
    val compressor = getForGlobalUsing
    compressor.setPreserveLineBreaks(false)
    compressor
  }


  /** Почта редкая, поэтому проще собирать компрессор каждый раз заново. */
  private def html4emailCompressor = {
    val compressor = getForGlobalUsing
    compressor.setRemoveIntertagSpaces(true)
    compressor.setRemoveMultiSpaces(true)
    //compressor.setCompressCss(true)
    compressor.setPreserveLineBreaks(false)
    compressor
  }


  /** SVG изредка сохраняются, поэтому выгоднее просто пересобирать компрессор каждый раз заново. */
  private def html4svgCompressor = {
    val compressor = getForGlobalUsing
    compressor.setRemoveIntertagSpaces(true)
    compressor.setPreserveLineBreaks(false)
    compressor.setRemoveComments(true)
    compressor.setRemoveMultiSpaces(true)
    compressor
  }

  /**
   * Прочитать svg из файла и сжать.
   *
   * @param f файл.
   * @return Сжатое содержимое.
   */
  def compressSvgFromFile(f: File): String = {
    val svgData = Files.readAllBytes(f.toPath)
    val svgText = new String(svgData)
    compressSvgText(svgText)
  }

  /**
   * Сжать svg-текст.
   *
   * @param svgText Текст svg xml.
   * @return Сжатый svg-текст.
   */
  def compressSvgText(svgText: String): String = html4svgCompressor.compress(svgText)

  def html2str4json(html: Html): String = {
    html4jsonCompressor.compress(html.body)
  }

  def html4email(html: Html): String = {
    html4emailCompressor.compress(html.body)
  }

  def html2jsStr(html: Html): JsString = {
    JsString( html2str4json(html) )
  }

  def txt2str(txt: Txt): String = txt.body.trim
  def txt2jsStr(txt: Txt): JsString = JsString( txt2str(txt) )

}


/** Реализация отключабельного play-фильтра. */
class HtmlCompressFilter @Inject() (
  hcu                         : HtmlCompressUtil,
  override val configuration  : Configuration,
  override val mat            : Materializer
)
  extends HTMLCompressorFilter
  with Filter
{
  override val compressor = hcu.getForGlobalUsing
}
