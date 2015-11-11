package util.adv

import com.google.inject.Inject
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import models._
import org.elasticsearch.client.Client
import play.api.Configuration
import util.PlayMacroLogsImpl

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.14 10:10
 * Description: Утиль для сбора карты экстра-ресиверов карточки по геородителям.
 */

/** Статическая утиль для вычисления списка экстра-ресиверов на основе гео-родительских связей. */
class AdvFreeGeoParentRcvrs @Inject() (
  mNodeCache                      : MAdnNodeCache,
  configuration                   : Configuration,
  implicit private val esClient   : Client,
  implicit private val ec         : ExecutionContext
)
  extends AdvExtraRcvrsCalculator
  with PlayMacroLogsImpl
{

  import LOGGER._

  /** Включено ли geo-parent авто-размещение? Фича была добавлена 2014.sep.12.
    * Её суть: бесплатная автопубликация гео-размещений на нижнем уровне всех геородителей. */
  override def isEnabled: Boolean = {
    configuration.getBoolean("adv.fwd.geo.parent.free.enabled")
      .getOrElse(true)
  }

  /**
   * Считаем ресиверов на основе карты непосредственных ресиверов.
   * @param allDirectRcvrs Карта непосредственных ресиверов.
   * @param producerIdOpt id продьюсера.
   * @return Фьючерс с картой extra-ресиверов.
   */
  override def calcForDirectRcvrs(allDirectRcvrs: Receivers_t, producerIdOpt: Option[String]): Future[Receivers_t] = {
    // Включено бесплатное авторазмещение на геородителях. Оставляем в карте только гео-размещения и анализируем.
    val geoRcvrIds = allDirectRcvrs
      .valuesIterator
      .filter { rcvrEdge =>
        !producerIdOpt.contains( rcvrEdge.nodeId )
      }
      .flatMap { rcvrInfo =>
        val sls1 = rcvrInfo.info
          .sls
          .iterator
          .filter(_.adnSink == AdnSinks.SINK_GEO)
        if (sls1.nonEmpty) {
          List(rcvrInfo.nodeId)
        } else {
          Nil
        }
      }
    val resultFut = calcFreeGeoParentRcvrs(geoRcvrIds)
    // Поделится радостью полученных данных с логгером:
    if (LOGGER.underlying.isTraceEnabled) {
      resultFut onSuccess { case result =>
        if (result.nonEmpty)
          trace("Found geoparent rcvrs: " + result.valuesIterator.mkString(", "))
      }
    }
    // Всё, вернуть результат.
    resultFut
  }


  /**
   * Рассчитать карту бесплатных размещений на гео-родительских узлах.
   * @param geoRcvrIds Множество непосредственных ресиверов (без ресивера саморазмещения!).
   * @return Карта только гео-родительских ресиверов, пригодная для последующего объединения
   *         с исходной картой.
   */
  def calcFreeGeoParentRcvrs(geoRcvrIds: TraversableOnce[String]): Future[Receivers_t] = {
    for {
      nodes <- mNodeCache.multiGet(geoRcvrIds)
    } yield {
      val sls = Set( SinkShowLevels.GEO_PRODUCER_SL )
      val eIter = nodes
        .iterator
        .flatMap { mnode =>
          mnode.edges.withPredicateIterIds( MPredicates.GeoParent)
        }
        .map { parentId =>
          MEdge(MPredicates.Receiver, parentId, info = MEdgeInfo(sls = sls))
        }
      MNodeEdges.edgesToMap1(eIter)
    }
  }

}
