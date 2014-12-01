package util.ai.mad

import java.io.{FileInputStream, InputStream}

import io.suggest.ym.model.MAd
import io.suggest.ym.model.ad.AOStringField
import models.ai.{MAiMad, MAiMadContentHandler, ContentHandlerResult}
import org.apache.tika.metadata.{TikaMetadataKeys, Metadata}
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper}
import org.apache.tika.parser.{ParseContext, AutoDetectParser}
import org.apache.tika.sax.TeeContentHandler
import org.clapper.scalasti.ST
import util.PlayMacroLogsImpl
import util.ws.HttpGetToFile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:00
 * Description: Утиль для обеспечения работоспособности автогенератора рекламных карточек.
 */
object MadAiUtil extends PlayMacroLogsImpl {

  /**
   * Привести HTTP-хидеры ответа к метаданным tika.
   * @param headers HTTP заголовки ответа.
   * @param meta Необязательный исходный аккамулятор метаданных tika.
   * @return Экземпляр Metadata.
   */
  def httpHeaders2meta(headers: Map[String, Seq[String]], urlOpt: Option[String], meta: Metadata = new Metadata): Metadata = {
    headers
      .iterator
      .flatMap { case (k, vs) => vs.iterator.map(v => (k, v)) }
      // TODO Выверять названия хидеров. Они могут приходить в нижнем регистре.
      .foreach { case (k, v) => meta.add(k, v) }
    if (urlOpt.isDefined)
      meta.add(TikaMetadataKeys.RESOURCE_NAME_KEY, urlOpt.get)
    meta
  }

  /**
   * Распарсить данные из файла с помощью tika. Нужно собрать список content-handler'ов под запрос и сделать дело.
   * @param is InputStream с контентом, подлежащим парсингу.
   * @param contentHandlers SAX-хандлеры для формирования карты шаблона.
   * @param meta Метаданные.
   * @return Результаты работы content-handler'ов в виде карты.
   */
  def parseFromStream(is: InputStream, contentHandlers: Seq[MAiMadContentHandler], meta: Metadata): Map[String, ContentHandlerResult] = {
    // Собираем все запрошенные парсеры
    val handlers = contentHandlers.map { _.newInstance }
    val handler = if (handlers.size > 1) {
      new TeeContentHandler(handlers: _*)
    } else {
      handlers.head
    }
    // Сборка цепочки парсинга
    val parser = new AutoDetectParser()   // TODO использовать HtmlParser? Да, если безопасно ли скармливать на вход HtmlParser'у левые данные.
    val parseContext = new ParseContext
    parseContext.set(classOf[HtmlMapper], new IdentityHtmlMapper)
    // Блокирующий запуск парсера. Заодно засекаем время его работы.
    val parserStartedAt: Long = if (LOGGER.underlying.isDebugEnabled) System.currentTimeMillis() else -1L
    try {
      parser.parse(is, handler, meta, parseContext)
    } finally {
      LOGGER.debug {
        val timeMs = System.currentTimeMillis() - parserStartedAt
        "tikaParseFromStream(): tika parser completed after " + timeMs + " ms."
      }
    }
    // Парсинг окончен. Пора собрать результаты.
    handlers
      .iterator
      .map { h => h.stiResKey -> h.getParseResult }
      .toMap
  }


  private def renderTextFieldOpt(tfOpt: Option[AOStringField], args: Map[String, ContentHandlerResult]): Option[AOStringField] = {
    tfOpt.map { tf =>
      val st = ST(tf.value, '#', '#')
        .addAttributes(args, raw = true)
      val v2 = st.render(lineWidth = 256)
      tf.copy(value = v2)
    }
  }

  /**
   * Рендер шаблонной карточки в финальную карточку испрользуя указанную карту аргументов.
   * @param tplAd Шаблонная карточка.
   * @param args Карта аргументов рендера.
   * @return Отрендеренная карточка: новый инстанс без id и версии.
   */
  def renderTplAd(tplAd: MAd, args: Map[String, ContentHandlerResult]): MAd = {
    tplAd.copy(
      offers = tplAd.offers.map { offer =>
        offer.copy(
          text1 = renderTextFieldOpt(offer.text1, args),
          text2 = renderTextFieldOpt(offer.text2, args)
        )
      },
      id          = None,
      versionOpt  = None,
      alienRsc    = true
    )
  }


  def processOne(madAi: MAiMad): Future[_] = {
    // Запустить получение результата по ссылки от remote-сервера.
    val getter = new HttpGetToFile {
      override def followRedirects: Boolean = false
      override def urlStr: String = madAi.url
    }
    val respFut = getter.request()
    // Запустить в фоне получение шаблонной карточки
    val tplMadFut = MAd.getById(madAi.tplAdId)
      .map(_.get)
    val renderArgsFut = respFut map { case (headers, file) =>
      // Получен результат в файл. Надо его распарсить в переменные для рендера.
      val meta = httpHeaders2meta(headers.headers, Some(madAi.url))
      val is = new FileInputStream(file)
      try {
        parseFromStream(is, madAi.contentHandlers, meta)
      } finally {
        is.close()
        file.delete()
      }
    }

    // Отрендерить шаблонную карточку.
    for {
      tplMad      <- tplMadFut
      renderArgs  <- renderArgsFut
    } yield {
      // Пробежаться по всем полям и вызвать рендерер.
      val madRendered = renderTplAd(tplMad, renderArgs)
      ???
      // TODO Сохранение новых id с защитой от удаление ресурсов.
    }
  }

}
