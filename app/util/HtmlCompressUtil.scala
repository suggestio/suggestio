package util

import java.io.File
import java.nio.file.Files

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import play.api.Play, Play.{current, configuration}
import play.twirl.api.HtmlFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 10:18
 * Description: Утиль для сжатия HTML-ответов.
 */
object HtmlCompressUtil {

  val PRESERVE_LINE_BREAKS_DFLT   = getBool("html.compress.global.preserve.line.breaks", Play.isDev)
  val REMOVE_COMMENTS_DFLT        = getBool("html.compress.global.remove.comments", Play.isProd)
  val REMOVE_INTERTAG_SPACES_DFLT = getBool("html.compress.global.remove.spaces.intertag", true)
  val STRIP_HTTP_PROTO            = getBool("html.compress.global.remove.proto.http", false)
  val STRIP_HTTPS_PROTO           = getBool("html.compress.global.remove.proto.https", false)

  def getForGlobalUsing = {
    val compressor = new HtmlCompressor()
    compressor.setPreserveLineBreaks(PRESERVE_LINE_BREAKS_DFLT)
    compressor.setRemoveComments(REMOVE_COMMENTS_DFLT)
    compressor.setRemoveIntertagSpaces(REMOVE_INTERTAG_SPACES_DFLT)
    // http false из-за проблем в iframe в demoWebSite.
    compressor.setRemoveHttpProtocol(STRIP_HTTP_PROTO)
    compressor.setRemoveHttpsProtocol(STRIP_HTTPS_PROTO)
    compressor
  }

  /** Прочитать булево значение из конфига через строку, которая может быть не только true/false. */
  private def getBool(confKey: String, dflt: Boolean): Boolean = {
    val result = configuration.getString(confKey).map {
      _.toLowerCase match {
        case "true"  | "1" | "+" | "yes" | "on" => true
        case "false" | "0" | "-" | "no" | "off" => false
        case "isprod" | "is_prod" => Play.isProd
        case "isdev" | "is_dev"   => Play.isDev
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
    compressor.setRemoveHttpProtocol(true)
    compressor.setRemoveHttpsProtocol(false)
    compressor
  }

  def compressForJson(html: HtmlFormat.Appendable) = html4jsonCompressor.compress(html.body)



  /** Почта редкая, поэтому проще собирать компрессор каждый раз заново. */
  private def html4emailCompressor = {
    val compressor = getForGlobalUsing
    compressor.setRemoveIntertagSpaces(true)
    compressor.setRemoveMultiSpaces(true)
    compressor.setRemoveHttpProtocol(false)
    compressor.setRemoveHttpsProtocol(false)
    //compressor.setCompressCss(true)
    compressor.setPreserveLineBreaks(false)
    compressor
  }

  def compressForEmail(html: HtmlFormat.Appendable) = html4emailCompressor.compress(html.body)



  /** SVG изредка сохраняются, поэтому выгоднее просто пересобирать компрессор каждый раз заново. */
  private def html4svgCompressor = {
    val compressor = getForGlobalUsing
    compressor.setRemoveIntertagSpaces(true)
    compressor.setRemoveHttpProtocol(false)
    compressor.setRemoveHttpsProtocol(false)
    compressor.setPreserveLineBreaks(false)
    compressor.setRemoveComments(true)
    compressor.setRemoveMultiSpaces(true)
    compressor
  }

  /**
   * Прочитать svg из файла и сжать.
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
   * @param svgText Текст svg xml.
   * @return Сжатый svg-текст.
   */
  def compressSvgText(svgText: String): String = html4svgCompressor.compress(svgText)

}

