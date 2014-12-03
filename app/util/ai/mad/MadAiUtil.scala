package util.ai.mad

import java.io.{FileInputStream, InputStream}

import io.suggest.ym.model.MAd
import models.ai.{MAiRenderer, MAiMad, MAiMadContentHandler, ContentHandlerResult}
import org.apache.tika.metadata.{TikaMetadataKeys, Metadata}
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper}
import org.apache.tika.parser.{ParseContext, AutoDetectParser}
import org.apache.tika.sax.TeeContentHandler
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
   * @param maim Экземпляр текущего MAiMad.
   * @param meta Метаданные.
   * @return Результаты работы content-handler'ов в виде карты.
   */
  def parseFromStream(is: InputStream, maim: MAiMad, meta: Metadata): Map[String, ContentHandlerResult] = {
    // Собираем все запрошенные парсеры
    val handlers = maim.contentHandlers.map { _.newInstance(maim) }
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


  /**
   * Рендер шаблонной карточки в финальную карточку испрользуя указанную карту аргументов и переданный набор парсеров.
   * @param tplAd Шаблонная карточка.
   * @param args Карта аргументов рендера.
   * @param renderers Рендереры в порядке употребления.
   * @param targetAds Целевые карточки, которые нужно обновить с помощью рендереров и карточки-шаблона.
   * @return Отрендеренные карточки в неопределённом порядке.
   */
  def renderTplAd(tplAd: MAd, renderers: Seq[MAiRenderer], args: Map[String, ContentHandlerResult], targetAds: Seq[MAd]): Future[Seq[MAd]] = {
    // Параллельно маппим все рекламные карточки. Это нарушает исходный порядок, но на это плевать.
    Future.traverse( targetAds ) { targetAd =>
      renderers.foldLeft(Future successful targetAd) {
        (acc, renderer) =>
          acc.flatMap { mad0 =>
            renderer
              .getRenderer()
              .renderTplAd(tplAd, args, targetAd = mad0)
          }
      }
    }
  }

  /**
   * Read-only запуск madAI на исполнение. Полезно для проверки валидности.
   * @param madAi id рекламной карточки.
   * @return Фьючерс с отрендеренными карточками, которые ещё не сохранены.
   */
  def dryRun(madAi: MAiMad): Future[Seq[MAd]] = {
    // Запустить получение результата по ссылки от remote-сервера.
    val getter = new HttpGetToFile {
      override def followRedirects = false
      override def urlStr = madAi.url
    }
    val respFut = getter.request()
    // Запустить в фоне получение шаблонной карточки
    val tplMadFut = MAd.getById(madAi.tplAdId)
      .map(_.get)
    val targetAdsFut = MAd.multiGet(madAi.targetAdIds)
      .filter { mads => mads.size == madAi.targetAdIds.size }
    val renderArgsFut = respFut map { case (headers, file) =>
      // Получен результат в файл. Надо его распарсить в переменные для рендера.
      val meta = httpHeaders2meta(headers.headers, Some(madAi.url))
      val is = new FileInputStream(file)
      try {
        parseFromStream(is, madAi, meta)
      } finally {
        is.close()
        file.delete()
      }
    }

    // Отрендерить шаблонную карточку с помощью цепочки рендереров.
    for {
      tplMad        <- tplMadFut
      renderArgs    <- renderArgsFut
      targetAds     <- targetAdsFut
      renders       <- renderTplAd(tplMad, madAi.renderers, renderArgs, targetAds)
    } yield {
      renders
    }
  }


  /**
   * Запуск на исполнение заполнятеля карточек на основе указанной спецификации.
   * @param madAi Данные по сборке карточек.
   * @return Фьючерс для синхронизации.
   */
  def run(madAi: MAiMad): Future[_] = {
    // Сохранить целевые карточки
    dryRun(madAi) flatMap { madsRendered =>
      Future.traverse(madsRendered)(_.save)
    }
  }

}


