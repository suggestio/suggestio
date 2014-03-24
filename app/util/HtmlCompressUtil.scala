package util

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import play.api.Play, Play.current
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
    compressor.setPreserveLineBreaks(Play.isDev)
    compressor.setRemoveComments(!Play.isDev)
    compressor.setRemoveIntertagSpaces(true)
    compressor.setRemoveHttpProtocol(true)
    compressor.setRemoveHttpsProtocol(true)
    compressor
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

