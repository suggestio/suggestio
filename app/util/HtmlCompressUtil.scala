package util

import java.io.File
import java.nio.file.Files

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import play.api.Play, Play.{current, configuration}
import play.api.http.{MimeTypes, HttpProtocol, HeaderNames}
import play.api.libs.iteratee.{Enumerator, Iteratee, Enumeratee}
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.twirl.api.{Html, HtmlFormat}

import scala.concurrent.Future

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


/** Реализация отключабельного play-фильтра. */
class HtmlCompressFilter
  extends HTMLCompressorFilter(HtmlCompressUtil.getForGlobalUsing)
  with Filter


/** Легковесный стрипатель пустот в начале текстов. Потом, может быть, вырастет в нечто бОльшее. */
// TODO Не заработал. Почему-то после подсчета длины результата внезапно начал резать текст в конце, а не в начале. Чудо.
class LightTextCompressFilter extends Filter {

  /** Статическая проверка на сжимабельность тела. */
  def isCompressable(result: Result): Boolean = {
    val hdrs = result.header.headers
    hdrs.get(HeaderNames.CONTENT_TYPE).exists(_ startsWith MimeTypes.HTML) &&
      !hdrs.get(HeaderNames.TRANSFER_ENCODING).exists(_ == HttpProtocol.CHUNKED) &&
      manifest[Enumerator[Html]].runtimeClass.isInstance(result.body)
  }

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh).flatMap { result =>
      if (isCompressable(result)) {
        // TODO var надо заменить аккамулятором функции. Только не очень-то ясно, как делать mapFold для enumerator'а.
        var isOnStart = true
        val body1 = result.body.map { bodyPart =>
          if (isOnStart) {
            // Можно и нужно быстро удалить пустые строки в начале. http://stackoverflow.com/a/14349083
            isOnStart = false
            // Использовать быстрый стрип whitespace'ов из http://stackoverflow.com/a/7668864
            var st = 0
            val l = bodyPart.length
            while(bodyPart(st) <= ' ' && st < l) {
              st += 1
            }
            java.util.Arrays.copyOfRange(bodyPart, st, l)
          } else {
            bodyPart
          }
        }
        // Нужно выставить новый content-lenght
        val cl = body1 |>>> Iteratee.fold[Array[Byte], Int](0) { (counter, bytes) =>
          counter + bytes.length
        }
        cl map { cl1 =>
          result.copy(
            body = body1,
            header = result.header.copy(
              headers = result.header.headers + (HeaderNames.CONTENT_LENGTH -> cl1.toString)
            )
          )
        }
      } else {
        Future successful result
      }
    }
  }
}
