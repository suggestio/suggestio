package util.tpl

import java.io.File
import java.nio.file.Files

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.github.fkoehler.play.htmlcompressor.HTMLCompressorFilter
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import io.suggest.playx.IsAppModes
import javax.inject.{Inject, Singleton}
import play.api.http.HttpEntity
import play.api.libs.json.JsString
import play.api.mvc.{EssentialAction, Filter, Result}
import play.api.{Configuration, Environment}
import play.twirl.api.{Html, Txt}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 10:18
 * Description: Утиль для сжатия HTML-ответов.
 */
@Singleton
class HtmlCompressUtil @Inject() (
  configuration   : Configuration,
  env             : Environment
)
  extends IsAppModes
{ outer =>

  override protected def appMode = env.mode

  private val PRESERVE_LINE_BREAKS_DFLT   = getBool("html.compress.global.preserve.line.breaks", isDev)
  private val REMOVE_COMMENTS_DFLT        = getBool("html.compress.global.remove.comments", isProd)
  private val REMOVE_INTERTAG_SPACES_DFLT = getBool("html.compress.global.remove.spaces.intertag", true)

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
    val result = configuration.getOptional[String](confKey).map {
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
                                     override implicit val mat   : Materializer,
                                     implicit private val ec     : ExecutionContext
                                   )
  extends HTMLCompressorFilter
  with Filter
{

  override val compressor = hcu.getForGlobalUsing

  // Чтобы избежать https://github.com/playframework/playframework/issues/8010
  // Далее в коде грязные костыли: перезапись перезаписанного apply() и копипаст приватного compressResult().

  // Делаем компрессию без аккамулятора. Пока не ясно, насколько безопасно это.
  override final def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { rh =>
      next(rh).mapFuture { result0 =>
        compressResult(result0)
      }
    }
  }

  /**
   * Extracted from CompressorFilter.scala:
   *
   * Compress the result.
   *
   * @param result The result to compress.
   * @return The compressed result.
   */
  private def compressResult(result: Result): Future[Result] = {
    def compress(data: ByteString) = compressor.compress(data.decodeString(charset).trim).getBytes(charset)

    if (isCompressible(result)) {
      result.body match {
        case body: HttpEntity.Strict =>
          Future.successful(
            result.copy(body = body.copy(ByteString(compress(body.data))))
          )
        case body: HttpEntity.Streamed =>
          for {
            bytes <- body.data
              .toMat(Sink.fold(ByteString())(_ ++ _))(Keep.right)
              // TODO Opt Тут материализация внутри Accumulator. Это плохо, потому что приводит к резкому падению производительности в неск.раз: https://github.com/playframework/playframework/issues/8010
              .run()
          } yield {
            val compressed = compress(bytes)
            val length = compressed.length
            result.copy(
              body = body.copy(
                data = Source.single(ByteString(compressed)),
                contentLength = Some(length.toLong)
              )
            )
          }
        case _ =>
          Future.successful(result)
      }
    } else {
      Future.successful(result)
    }
  }

}
