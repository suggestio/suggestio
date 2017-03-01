package util.showcase

import java.time.LocalDate

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import controllers.routes
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import models.mctx.{Context, ContextUtil}
import models.msc.ScJsState
import play.api.mvc.QueryStringBindable
import util.n2u.N2NodesUtil
import util.seo.SiteMapXmlCtl

import scala.collection.immutable


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
  ctxUtil                       : ContextUtil
)
  extends SiteMapXmlCtl
{

  import mNodes.Implicits._

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlEnumerator(implicit ctx: Context): Source[SiteMapUrlT, _] = {
    val adSearch = new MNodeSearchDfltImpl {

      override def isEnabled = Some(true)

      override def nodeTypes: Seq[MNodeType] = {
        MNodeTypes.AdnNode ::
          MNodeTypes.Ad ::
          // TODO Теги тоже надо индексировать, по идее. Но надо разобраться с выдачей по-лучше на предмет тегов, URL и их заголовков.
          Nil
      }

      override def outEdges: Seq[ICriteria] = {
        val preds = MPredicates.AdvGeoPlace ::
          MPredicates.Receiver ::
          MPredicates.TaggedBy.Agt ::
          MPredicates.TaggedBy.DirectTag ::
          MPredicates.AdnMap ::
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

    // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
    val today = LocalDate.now()
    val qsb = ScJsState.qsbStandalone

    src0.mapConcat { mad =>
      mad2sxu(mad, today, qsb)
    }
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
                       (implicit ctx: Context): List[SiteMapUrl] = {
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

    sxuOpt.toList
  }

}
