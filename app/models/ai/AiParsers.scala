package models.ai

import java.io.{FileInputStream, File, InputStreamReader, InputStream}

import io.suggest.model.EnumMaybeWithName
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper}
import org.apache.tika.parser.{ParseContext, AutoDetectParser}
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import play.api.libs.ws.WSResponseHeaders
import util.PlayLazyMacroLogsImpl
import util.parse.{TikaParseUtil, SaxParseUtil}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.14 10:45
 * Description: Парсеры, используемые для Ai-функционала.
 */

object AiParsers extends Enumeration with EnumMaybeWithName with PlayLazyMacroLogsImpl {

  /**
   * Экземпляр модели парсера.
   * @param name Название.
   */
  protected abstract class Val(val name: String) extends super.Val(name) {
    /**
     * Выполнить парсинг из файла.
     * @param handler Обработчик контента.
     * @param ctx Контекст парсера.
     * @return Фьючерс для синхронизации. Ожидается, что вся полезная инфа осядет внутри переданного handler'а.
     */
    def parseFromStream(handler: DefaultHandler, ctx: MAiParserCtxT): Unit
  }

  type AiParser = Val
  override type T = AiParser


  /** Tika-парсер. Подходит для разных форматов, кроме валидных xml и прочих rss. */
  val Tika: AiParser = new Val("tika") {
    override def parseFromStream(handler: DefaultHandler, ctx: MAiParserCtxT): Unit = {
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
    }
  }


  /** Толерантный SAX-парсер. Подходит для разных XML-форматов. */
  val SaxTolerant: AiParser = new Val("saxt") {
    override def parseFromStream(handler: DefaultHandler, ctx: MAiParserCtxT): Unit = {
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

