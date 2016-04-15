package util.ai.mad

import java.io.FileInputStream

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.node.MNodes
import models.MNode
import models.ai._
import org.apache.tika.metadata.{Metadata, TikaMetadataKeys}
import org.apache.tika.sax.TeeContentHandler
import org.clapper.scalasti.ST
import org.elasticsearch.client.Client
import play.api.libs.ws.WSClient
import util.PlayMacroLogsImpl
import util.ws.HttpGetToFile

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:00
 * Description: Утиль для обеспечения работоспособности автогенератора рекламных карточек.
 */
class MadAiUtil @Inject() (
  mNodes                  : MNodes,
  implicit val ws1        : WSClient,
  implicit val ec         : ExecutionContext,
  implicit val esClient   : Client,
  implicit val sn         : SioNotifierStaticClientI
)
  extends PlayMacroLogsImpl
{

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
   * Распарсить данные из потока с помощью tika. Нужно собрать список content-handler'ов под запрос и сделать дело.
   * @param parseCtx Неизменяемый контекст парсинга.
   * @return Результаты работы content-handler'ов в виде карты.
   */
  def parseSource(chs: Seq[MAiMadContentHandler], parseCtx: MAiParserCtxT): Future[Map[String, ContentHandlerResult]] = {
    // Собираем все запрошенные парсеры
    // 2014.dec.04: Каждый ContentHandler сам выбирает себе парсер из модели парсеров. Теперь поддерживается не только Tika.
    // Если задействованы разные парсеры, то они будут выполняться параллельно.
    val running = chs
      .map { _.newInstance(parseCtx) }
      .groupBy(_.sourceParser)
      .map { case (parser, handlers) =>
        Future {
          val pch = if (handlers.size > 1) {
            new TeeContentHandler(handlers: _*)
          } else {
            handlers.head
          }
          parser.parseFromStream(pch, parseCtx)
          // Парсинг окончен. Пора собрать результаты.
          handlers
            .iterator
            .map { h => h.stiResKey -> h.getParseResult}
            .toMap
        }
      }
    Future.reduce(running)(_ ++ _)
  }


  /**
   * Рендер шаблонной карточки в финальную карточку испрользуя указанную карту аргументов и переданный набор парсеров.
   * @param tplAd Шаблонная карточка.
   * @param args Карта аргументов рендера.
   * @param renderers Рендереры в порядке употребления.
   * @param targetAds Целевые карточки, которые нужно обновить с помощью рендереров и карточки-шаблона.
   * @return Отрендеренные карточки в неопределённом порядке.
   */
  def renderTplAd(tplAd: MNode, renderers: Seq[MAiRenderer], args: Map[String, ContentHandlerResult],
                  targetAds: Seq[MNode]): Future[Seq[MNode]] = {
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
  def dryRun(madAi: MAiMad): Future[Seq[MNode]] = {
    // Запустить получение результата по ссылки от remote-сервера.
    val urlRenderCtx = new UrlRenderContextBeanImpl()
    val renderArgsFut = Future.traverse( madAi.sources ) { source =>
      // 2014.12.03: Поддержка URL, который содержит переменные, описанные через ScalaSti-синтаксис.
      val url = ST(source.url, '#', '#')
        .add("ctx", urlRenderCtx, raw = true)
        .render()
      val getter = new HttpGetToFile {
        override def ws = ws1
        override def followRedirects = false
        override def urlStr = url
      }
      getter.request().flatMap { case (headers, file) =>
        // Запускаем в фоне парсинг и обработку входных данных.
        val parseCtx = new MAiParserCtxT with MAiCtxWrapper {
          override def openInputStream  = new FileInputStream(file)
          override def urlOpt           = Some(url)
          override def respHeaders      = headers.headers
          override def mAiCtx           = madAi
        }
        parseSource(source.contentHandlers, parseCtx)
      }
    } map { results =>
      results.reduce(_ ++ _)
    }

    // Запустить в фоне получение шаблонной карточки
    val tplMadFut = mNodes.getById(madAi.tplAdId)
      .map(_.get)
    val targetAdsFut = mNodes.multiGetRev(madAi.targetAdIds)
      .filter { mads => mads.size == madAi.targetAdIds.size}

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
    dryRun(madAi).flatMap { madsRendered =>
      Future.traverse(madsRendered)(mNodes.save)
    }
  }

}


