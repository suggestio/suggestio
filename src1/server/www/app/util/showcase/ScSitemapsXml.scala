package util.showcase

import java.time.LocalDate

import akka.stream.scaladsl.Source
import javax.inject.Inject
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.spa.SioPages
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import models.crawl.{ChangeFreqs, SiteMapUrl}
import models.mctx.ContextUtil
import play.api.inject.Injector
import util.seo.SiteMapXmlCtl


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.11.14 17:37
 * Description: Выдача - основная составляющая sitemap'a. Тут система сбора карточек и отображения
 * карточек на sitemap'у.
 * Вычисление значений changefreq проходит по направлению оптимального кравлинга:
 * - Если изменялось сегодня, то hourly. Это простимулирует кравлер просмотреть документ сегодня.
 * - Если изменилось вчера или ранее, то значение lastmod уведомит кравлер, что страница изменилась.
*/
class ScSitemapsXml @Inject() (
                                injector                      : Injector,
                              )
  extends SiteMapXmlCtl
  with MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val ctxUtil = injector.instanceOf[ContextUtil]

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlSrc(): Source[SiteMapUrl, _] = {
    val adSearch = new MNodeSearch {

      override val isEnabled = Some(true)

      override val nodeTypes: Seq[MNodeType] = {
        MNodeTypes.Ad ::
          // TODO Теги тоже надо индексировать, по идее. Но надо разобраться с выдачей по-лучше на предмет тегов, URL и их заголовков.
          // TODO А что с узлами-ресиверами, с плиткой которые?
          Nil
      }

      override val outEdges: MEsNestedSearch[Criteria] = {
        val preds = MPredicates.AdvGeoPlace ::
          MPredicates.Receiver ::
          MPredicates.TaggedBy.AdvGeoTag ::
          MPredicates.TaggedBy.DirectTag ::
          Nil
        MEsNestedSearch.plain(
          (for (p <- preds) yield {
            Criteria(
              predicates = p :: Nil,
            )
          }): _*,
        )
      }

      // Кол-во узлов за одну порцию.
      override def limit = 25
    }

    lazy val logPrefix = s"siteMapXmlSrc()[${System.currentTimeMillis()}]:"

    // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
    val today = LocalDate.now()

    import mNodes.Implicits._
    import streamsUtil.Implicits._
    import esModel.api._

    mNodes
      .source[MNode]( adSearch.toEsQuery )
      .mapConcat { mad =>
        try {
          mad2sxu(mad, today)
        } catch {
          // Подавить возможные ошибки рендера ссылок для текущего узла:
          case ex: Throwable =>
            LOGGER.error(s"$logPrefix Failed to render sitemap URL from node#${mad.id.orNull}", ex)
            Nil
        }
      }
      // Записать в логи кол-во пройденных узлов. Обычно оно эквивалентно кол-ву сгенеренных URL.
      .maybeTraceCount(this) { totalCount =>
        s"$logPrefix Total nodes found: $totalCount"
      }
  }



  /**
   * Приведение рекламной карточки к элементу sitemap.xml.
   * @param mad Экземпляр рекламной карточки.
   * @param today Дата сегодняшнего дня.
   * @return Экземпляры SiteMapUrl.
   *         Если карточка на годится для индексации, то пустой список.
   */
  protected def mad2sxu(mad: MNode, today: LocalDate): List[SiteMapUrl] = {

    val rcvrIdOpt = mad.edges
      .withPredicateIter(MPredicates.Receiver, MPredicates.OwnedBy)
      .flatMap(_.nodeIds)
      .nextOption()

    // Поиска текущую геоточку, если карточка там размещена, и на узле её не отобразить.
    val gpOpt = if (rcvrIdOpt.isEmpty) {
      mad.edges
        .withPredicateIter( MPredicates.AdvGeoPlace )
        .flatMap( _.info.geoPoints )
        .nextOption()
    } else {
      // Узел-ресивер уже есть. Точка рендерить не требуется.
      None
    }

    // TODO Учитывать геотеги? direct-теги?

    // Собрать данные для sitemap-ссылки на карточку.
    val jsState = SioPages.Sc3(
      nodeId       = rcvrIdOpt,
      focusedAdId  = mad.id,
      generation   = None, // Всем юзерам поисковиков будет выдаваться одна ссылка, но всегда на рандомную выдачу.
      locEnv       = gpOpt
    )

    val url = controllers.sc.routes.ScSite.geoSite(jsState).url
    val lastDt = mad.meta.basic.dateEditedOrCreated
    val lastDate = lastDt.toLocalDate

    val smu = SiteMapUrl(
      loc         = ctxUtil.SC_URL_PREFIX + url,
      lastMod     = Some(lastDate),
      changeFreq  = Some {
        if (lastDate isBefore today)
          ChangeFreqs.daily
        else
          ChangeFreqs.hourly
      }
    )

    smu :: Nil
  }

}
