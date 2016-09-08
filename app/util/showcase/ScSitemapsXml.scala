package util.showcase

import com.google.inject.Inject
import controllers.routes
import io.suggest.model.es.EsModelUtil
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.MNodes
import io.suggest.util.SioEsUtil.laFuture2sFuture
import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import models.mctx.{Context, ContextUtil}
import models.mproj.MCommonDi
import models.msc.ScJsState
import models.{AdSearchImpl, MNode}
import org.elasticsearch.common.unit.TimeValue
import org.joda.time.LocalDate
import play.api.libs.iteratee.Enumerator
import play.api.mvc.QueryStringBindable
import util.n2u.N2NodesUtil
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
  n2NodesUtil                   : N2NodesUtil,
  mNodes                        : MNodes,
  ctxUtil                       : ContextUtil,
  mCommonDi                     : MCommonDi
)
  extends SiteMapXmlCtl
{

  import mCommonDi._

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT] = {
    val adSearch = new AdSearchImpl {
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates = Seq( MPredicates.OwnedBy ),
          anySl = Some(true)
        )
        Seq(cr)
      }
    }
    var reqb = mNodes.dynSearchReqBuilder(adSearch)
    reqb = mNodes.prepareScroll(srb = reqb)

    // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
    val today = LocalDate.now()
    val qsb = ScJsState.qsbStandalone

    // Запускаем поточный обход всех опубликованных MAd'ов и поточную генерацию sitemap'а.
    val erFut = reqb.execute().map { searchResp0 =>
      // TODO Запихать это всё в модели.
      // Пришел ответ без результатов с начальным scrollId.
      Enumerator.unfoldM(searchResp0.getScrollId) { scrollId0 =>
        esClient.prepareSearchScroll(scrollId0)
          .setScroll(new TimeValue(EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT))
          .execute()
          .map { sr =>
            val scrollId = sr.getScrollId
            if (sr.getHits.getHits.isEmpty) {
              esClient.prepareClearScroll()
                .addScrollId(scrollId)
                .execute()
              None
            } else {
              Some(scrollId, mNodes.searchResp2list(sr))
            }
          }
      }.flatMap { mads =>
        // Из списка mads делаем Enumerator, который поочерёдно запиливает каждую карточку в элемент sitemap.xml.
        Enumerator(mads : _*)
          .flatMap[SiteMapUrlT] { mad =>
            Enumerator(mad2sxu(mad, today, qsb) : _*)
          }
      }
    }
    Enumerator.flatten(erFut)
  }


  /**
   * Приведение рекламной карточки к элементу sitemap.xml.
   * @param mad Экземпляр рекламной карточки.
   * @param today Дата сегодняшнего дня.
   * @param ctx Контекст.
   * @return Экземпляры SiteMapUrl.
   *         Если карточка на годится для индексации, то пустой список.
   */
  protected def mad2sxu(mad: MNode, today: LocalDate, qsb: QueryStringBindable[ScJsState])
                       (implicit ctx: Context): Seq[SiteMapUrl] = {
    val sxuOpt = for {
      // Нужны карточки только с продьюсером.
      producerId  <- n2NodesUtil.madProducerId(mad)
      // Выбираем, на каком ресивере отображать карточку.
      extRcvrEdge <- {
        mad.edges
          .withPredicateIter(MPredicates.Receiver)
          .filter { e =>
            e.nodeIds.nonEmpty && !e.nodeIds.contains(producerId) &&  e.info.sls.nonEmpty
          }
          .toStream
          .headOption
      }

    } yield {
      // Собрать данные для sitemap-ссылки на карточку.
      val jsState = ScJsState(
        adnId           = extRcvrEdge.nodeIds.headOption,   // TODO headOption -- нужно понять, безопасно ли это тут. edge.nodeIds стало коллекцией внезапно.
        fadOpenedIdOpt  = mad.id,
        generationOpt   = None // Всем юзерам поисковиков будет выдаваться одна ссылка, но всегда на рандомную выдачу.
      )
      val url = routes.Sc.geoSite().url + "#!?" + jsState.toQs(qsb)
      val lastDt = mad.meta.basic.dateEditedOrCreated
      val lastDate = lastDt.toLocalDate
      SiteMapUrl(
        // TODO Нужно здесь перейти на #!-адресацию, когда появится поддержка этого чуда в js выдаче.
        loc         = ctxUtil.SC_URL_PREFIX + url,
        lastMod     = Some(lastDate),
        changeFreq  = Some {
          if (lastDate isBefore today)
            ChangeFreqs.daily
          else
            ChangeFreqs.hourly
        }
      )
    }

    sxuOpt.toSeq
  }

}
