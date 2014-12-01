package util.ai.mad

import java.io.{InputStream, File}
import java.util.concurrent.Callable

import models.ai.MAiMad
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper}
import org.apache.tika.parser.{ParseContext, AutoDetectParser}
import util.PlayMacroLogsImpl
import util.ai.sax.weather.gidromet.GidrometRssSax

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:00
 * Description: Утиль для обеспечения работоспособности автогенератора рекламных карточек.
 */
object MadAiUtil {

  /**
   * Привести HTTP-хидеры ответа к метаданным tika.
   * @param headers HTTP заголовки ответа.
   * @param meta Необязательный исходный аккамулятор метаданных tika.
   * @return Экземпляр Metadata.
   */
  def httpHeaders2meta(headers: Map[String, Seq[String]], meta: Metadata = new Metadata): Metadata = {
    headers
      .iterator
      .flatMap { case (k, vs) => vs.iterator.map(v => (k, v)) }
      // TODO Выверять названия хидеров. Они могут приходить в нижнем регистре.
      .foreach { case (k, v) => meta.add(k, v) }
    meta
  }

  /**
   * Распарсить данные из файла с помощью tika. Нужно собрать список content-handler'ов под запрос и сделать дело.
   * @param is InputStream с контентом, подлежащим парсингу.
   * @param aiMad Описание задачи AI для сборки карточек.
   * @param meta Метаданные.
   * @return Результаты работы content-handler'ов.
   */
  def parseFromStream(is: InputStream, aiMad: MAiMad, meta: Metadata) = {

    ???
  }

}


class TikaCallable(md: Metadata, input: InputStream) extends Callable[Map[String, AnyRef]] with PlayMacroLogsImpl {

  import LOGGER._

  def logPrefix = "TikaCallable"

  override def call(): Map[String, AnyRef] = {
    // Сборка цепочки парсинга
    val saxHandler = new GidrometRssSax
    val parser = new AutoDetectParser()   // TODO использовать HtmlParser? Да, если безопасно ли скармливать на вход HtmlParser'у левые данные.
    val parseContext = new ParseContext
    parseContext.set(classOf[HtmlMapper], new IdentityHtmlMapper)
    // Блокирующий запуск парсера. Заодно засекаем время его работы.
    val parserStartedAt: Long = if (LOGGER.underlying.isDebugEnabled) System.currentTimeMillis() else -1L
    try {
      parser.parse(input, saxHandler, md, parseContext)
    } finally {
      // Выполнено. Не исключено, что с ошибкой, ибо вместо HTML может прийти всё что угодно.
      LOGGER.debug {
        val timeMs = System.currentTimeMillis() - parserStartedAt
        logPrefix + "tika parser completed after " + timeMs + " ms."
      }
    }
    ???
  }
}
