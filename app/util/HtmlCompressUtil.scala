package util

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import play.api.Play, Play.{current, configuration}
import play.api.templates.HtmlFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 10:18
 * Description: Утиль для сжатия HTML-ответов.
 */
object HtmlCompressUtil {

  def getForGlobalUsing = {
    val compressor = new HtmlCompressor()
    compressor.setPreserveLineBreaks(
      getBool("html.compress.global.preserve.line.breaks", Play.isDev))
    compressor.setRemoveComments(
      getBool("html.compress.global.remove.comments", Play.isProd))
    compressor.setRemoveIntertagSpaces(
      getBool("html.compress.global.remove.spaces.intertag", true))
    // http false из-за проблем в iframe в demoWebSite.
    compressor.setRemoveHttpProtocol(
      getBool("html.compress.global.remove.proto.http", false))
    compressor.setRemoveHttpsProtocol(
      getBool("html.compress.global.remove.proto.https", true))
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


  private val html4jsonCompressor = {
    val compressor = getForGlobalUsing
    compressor.setPreserveLineBreaks(false)
    compressor.setRemoveHttpProtocol(true)
    compressor.setRemoveHttpsProtocol(false)
    compressor
  }

  def compressForJson(html: HtmlFormat.Appendable) = html4jsonCompressor.compress(html.body)


  private val html4emailCompressor = {
    val compressor = getForGlobalUsing
    compressor.setRemoveIntertagSpaces(false)
    compressor.setRemoveMultiSpaces(true)
    compressor.setRemoveHttpProtocol(false)
    compressor.setRemoveHttpsProtocol(false)
    //compressor.setCompressCss(true)
    compressor.setPreserveLineBreaks(false)
    compressor
  }

  def compressForEmail(html: HtmlFormat.Appendable) = html4emailCompressor.compress(html.body)

}

