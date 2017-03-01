package util.showcase

import java.time.LocalDate

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import controllers.routes
import io.suggest.async.StreamsUtil
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.MacroLogsImpl
import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import models.mctx.ContextUtil
import models.mproj.ICommonDi
import models.msc.ScJsState
import play.api.mvc.QueryStringBindable
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
  mNodes                        : MNodes,
  streamsUtil                   : StreamsUtil,
  ctxUtil                       : ContextUtil,
  mCommonDi                     : ICommonDi
)
  extends SiteMapXmlCtl
  with MacroLogsImpl
{

  import mNodes.Implicits._
  import mCommonDi.ec

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlSrc(): Source[SiteMapUrlT, _] = {
    val adSearch = new MNodeSearchDfltImpl {

      override def isEnabled = Some(true)

      override def nodeTypes: Seq[MNodeType] = {
        MNodeTypes.Ad ::
          // TODO Теги тоже надо индексировать, по идее. Но надо разобраться с выдачей по-лучше на предмет тегов, URL и их заголовков.
          // TODO А что с узлами-ресиверами, с плиткой которые?
          Nil
      }

      override def outEdges: Seq[ICriteria] = {
        val preds = MPredicates.AdvGeoPlace ::
          MPredicates.Receiver ::
          MPredicates.TaggedBy.Agt ::
          MPredicates.TaggedBy.DirectTag ::
          Nil
        for (p <- preds) yield {
          Criteria(
            predicates = p :: Nil
          )
        }
      }

      // Кол-во узлов за одну порцию.
      override def limit = 25
    }

    val src0 = mNodes.source[MNode](adSearch)

    lazy val logPrefix = s"siteMapXmlSrc()[${System.currentTimeMillis()}]:"

    // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
    val today = LocalDate.now()
    val qsb = ScJsState.qsbStandalone

    val urls = src0
      .mapConcat { mad =>
        try {
          mad2sxu(mad, today, qsb)
        } catch {
          // Подавить возможные ошибки рендера ссылок для текущего узла:
          case ex: Throwable =>
            LOGGER.error(s"$logPrefix Failed to render sitemap URL from node#${mad.id.orNull}", ex)
            Nil
        }
      }

    // Записать в логи кол-во пройденных узлов. Обычно оно эквивалентно кол-ву сгенеренных URL.
    if ( LOGGER.underlying.isTraceEnabled() ) {
      for (totalCount <- streamsUtil.count(src0)) yield {
        LOGGER.trace(s"$logPrefix Total nodes found: $totalCount")
      }
    }

    urls
  }



  /**
   * Приведение рекламной карточки к элементу sitemap.xml.
   * @param mad Экземпляр рекламной карточки.
   * @param today Дата сегодняшнего дня.
   * @return Экземпляры SiteMapUrl.
   *         Если карточка на годится для индексации, то пустой список.
   */
  protected def mad2sxu(mad: MNode, today: LocalDate, qsb: QueryStringBindable[ScJsState]): List[SiteMapUrl] = {

    val rcvrIdOpt = mad.edges
      .withPredicateIter(MPredicates.Receiver, MPredicates.OwnedBy)
      .flatMap(_.nodeIds)
      .toStream
      .headOption

    // Поиска текущую геоточку, если карточка там размещена, и на узле её не отобразить.
    val gpOpt = if (rcvrIdOpt.isEmpty) {
      mad.edges
        .withPredicateIter( MPredicates.AdvGeoPlace )
        .flatMap( _.info.geoPoints )
        .toStream
        .headOption
    } else {
      // Узел-ресивер уже есть. Точка рендерить не требуется.
      None
    }

    // TODO Учитывать геотеги? direct-теги?

    // Собрать данные для sitemap-ссылки на карточку.
    val jsState = ScJsState(
      adnId           = rcvrIdOpt,
      fadOpenedIdOpt  = mad.id,
      generationOpt   = None, // Всем юзерам поисковиков будет выдаваться одна ссылка, но всегда на рандомную выдачу.
      geoPoint        = gpOpt
    )

    val url = routes.Sc.geoSite().url + "#!?" + jsState.toQs(qsb)
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
