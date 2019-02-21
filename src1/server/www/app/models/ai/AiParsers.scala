package models.ai

import java.io.InputStream

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq
import org.apache.tika.parser.html.{HtmlMapper, IdentityHtmlMapper}
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import util.parse.{SaxParseUtil, TikaParseUtil}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.14 10:45
 * Description: Парсеры, используемые для Ai-функционала.
 */

object AiParsers extends StringEnum[AiParser] {

  case object Tika extends AiParser("tika")

  case object SaxTolerant extends AiParser("saxt")


  override def values = findValues

}


sealed abstract class AiParser(override val value: String) extends StringEnumEntry


object AiParser {

  @inline implicit def univEq: UnivEq[AiParser] = UnivEq.derive


  implicit class AiParserOpsExt(val aiParser: AiParser) extends AnyVal {

    /**
      * Выполнить парсинг из файла.
      * @param handler Обработчик контента.
      * @param ctx Контекст парсера.
      * @return Фьючерс для синхронизации. Ожидается, что вся полезная инфа осядет внутри переданного handler'а.
      */
    def parseFromStream(handler: DefaultHandler, ctx: MAiParserCtxT): Unit = {
      aiParser match {
        /** Tika-парсер. Подходит для разных форматов, кроме валидных xml и прочих rss. */
        case AiParsers.Tika =>
          val parser = new AutoDetectParser()   // TODO использовать HtmlParser? Да, если безопасно ли скармливать на вход HtmlParser'у левые данные.
          val parseContext = new ParseContext
          parseContext.set(classOf[HtmlMapper], new IdentityHtmlMapper)
          val meta = TikaParseUtil.httpHeaders2meta(ctx.respHeaders, ctx.urlOpt)
          val is = ctx.openInputStream
          try {
            parser.parse(is, handler, meta, parseContext)
          } finally {
            is.close()
          }

        /** Толерантный SAX-парсер. Подходит для разных XML-форматов. */
        case AiParsers.SaxTolerant =>
          val p = ctx.saxFactory.newSAXParser()
          // В ЦБ РФ не осилили Unicode, поэтому нужно детектить кодировку на ходу http://stackoverflow.com/a/3482791
          val is = ctx.openInputStream
          try {
            val isource = new InputSource(is)
            p.parse(isource, handler)
          } finally {
            is.close()
          }
      }
    }

  }

}


/** Интерфейс контекста вызова парсера. Он содержит какую-то инфу о предшествующих действиях подсистемы Ai. */
trait MAiParserCtxT extends MAiCtx {
  /** Открыть новый входной поток данных. Пользователь этого метода должен обязательно позабоиться о его закрытии. */
  def openInputStream: InputStream

  /** Опциональная ссылка на источник данных. */
  def urlOpt: Option[String]

  /** Заголовки ответа или иная/пустая карта метаданных, совместимых с HTTP-заголовками. */
  def respHeaders: Map[String, Seq[String]]

  /** Для парсеров, базирующихся на javax.xml можно шарить инстанс SAXFactory. */
  lazy val saxFactory = SaxParseUtil.getSaxFactoryTolerant
}

