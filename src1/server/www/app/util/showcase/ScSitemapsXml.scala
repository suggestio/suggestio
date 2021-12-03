package util.showcase

import java.time.LocalDate
import akka.stream.scaladsl.Source
import io.suggest.common.empty.OptionUtil

import javax.inject.Inject
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.spa.SioPages
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import models.crawl.{ChangeFreqs, SiteMapUrl}
import models.mctx.{Context, ContextUtil}
import play.api.inject.Injector
import util.seo.SiteMapXmlCtl

import scala.concurrent.ExecutionContext


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
  private implicit lazy val ec = injector.instanceOf[ExecutionContext]


  /** Sometimes, it is needed to recrawl the site after some major change.
    * This lower date bound, helps to achieve recrawl: type here last major change date for all pages. */
  private val LAST_MODIFIED_DATE_AT_LEAST: Option[LocalDate] =
    Some( LocalDate.of(2021, 12, 3) )

  private val nodeAdvPreds: List[MPredicate] =
    MPredicates.Receiver ::
    MPredicates.TaggedBy.DirectTag ::
    Nil

  private val allAdvPreds: List[MPredicate] =
    MPredicates.AdvGeoPlace ::
    MPredicates.TaggedBy.AdvGeoTag ::
    nodeAdvPreds

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlSrc()(implicit ctx: Context): Source[SiteMapUrl, _] = {
    Source.lazyFutureSource { () =>
      for {
        domainNode3pOpt <- ctx.domainNode3pOptFut
      } yield {
        val rcvrId3p = domainNode3pOpt.flatMap(_.id)

        val adSearch = new MNodeSearch {

          override val isEnabled = OptionUtil.SomeBool.someTrue

          override val nodeTypes: Seq[MNodeType] = {
            MNodeTypes.Ad ::
              // TODO Теги тоже надо индексировать, по идее. Но надо разобраться с выдачей по-лучше на предмет тегов, URL и их заголовков.
              // TODO А что с узлами-ресиверами, с плиткой которые?
              Nil
          }

          override val outEdges: MEsNestedSearch[Criteria] = {
            val crs: Seq[Criteria] = if (rcvrId3p.isEmpty) {
              // Render all suggest.io sitemap:
              for (p <- allAdvPreds) yield {
                Criteria(
                  predicates = p :: Nil,
                )
              }

            } else {
              // Criterias only for 3p-domain receivers:
              for {
                nodeAdvPred <- nodeAdvPreds
              } yield {
                Criteria(
                  predicates      = nodeAdvPred :: Nil,
                  nodeIds         = rcvrId3p.toList,
                  nodeIdsMatchAll = false,
                )
              }
            }

            MEsNestedSearch.plain( crs: _* )
          }

          // Кол-во узлов за одну порцию.
          override def limit = 25
        }

        lazy val logPrefix = s"siteMapXmlSrc()[${System.currentTimeMillis()}]:"

        // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
        val today = LocalDate.now()

        val siteMapUrlPrefix = domainNode3pOpt.fold( ctxUtil.SC_URL_PREFIX ) { _ =>
          ctxUtil.PROTO + "://" + ctx.request.host
        }

        import mNodes.Implicits._
        import streamsUtil.Implicits._
        import esModel.api._

        val scAdsSrc = mNodes
          .source[MNode]( adSearch.toEsQuery )
          .mapConcat { mad =>
            try {
              nodeToSiteMapUrl( mad, today, siteMapUrlPrefix, rcvrId3p )
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

        val staticsSrc = staticSiteMap( siteMapUrlPrefix, rcvrId3p )

        staticsSrc ++ scAdsSrc
      }
    }
  }


  /** Render some static and special non-rdbms page-urls for indexing. */
  private def staticSiteMap(urlPrefix: String, rcvrId3p: Option[String]): Source[SiteMapUrl, _] = {
    var acc = List.empty[SiteMapUrl]

    // Don't index suggest.io offero and privacy links for 3p-domains:
    if (rcvrId3p.isEmpty) {
      acc ::= SiteMapUrl(
        loc = urlPrefix + controllers.routes.Static.offero().url,
        changeFreq = Some( ChangeFreqs.monthly ),
      )
      acc ::= SiteMapUrl(
        loc = urlPrefix + controllers.routes.Static.privacyPolicy().url,
        changeFreq = Some( ChangeFreqs.yearly ),
      )
    }

    // Add current domain main page:
    acc ::= SiteMapUrl(
      loc = urlPrefix + controllers.sc.routes.ScSite.geoSite().url,
      // By now, 3p-domains changes rare.
      changeFreq = Some( if (rcvrId3p.isEmpty) ChangeFreqs.hourly else ChangeFreqs.daily ),
    )

    Source( acc )
  }



  /**
   * Приведение рекламной карточки к элементу sitemap.xml.
   * @param mad Экземпляр рекламной карточки.
   * @param today Дата сегодняшнего дня.
   * @return Экземпляры SiteMapUrl.
   *         Если карточка на годится для индексации, то пустой список.
   */
  private def nodeToSiteMapUrl(mad: MNode, today: LocalDate, urlPrefix: String, rcvrId3p: Option[String]): List[SiteMapUrl] = {
    val rcvrIdOpt = rcvrId3p orElse {
      mad.edges
        .withPredicateIter( nodeAdvPreds: _* )
        .flatMap(_.nodeIds)
        .nextOption()
    }

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

    val lastDt = mad.meta.basic.dateEditedOrCreated
    var lastDate = lastDt.toLocalDate
    for {
      minDate <- LAST_MODIFIED_DATE_AT_LEAST
      if lastDate isBefore minDate
    } {
      lastDate = minDate
    }

    val smu = SiteMapUrl(
      loc         = urlPrefix + controllers.sc.routes.ScSite.geoSite(jsState).url,
      lastMod     = Some( lastDate ),
    )

    smu :: Nil
  }

}
