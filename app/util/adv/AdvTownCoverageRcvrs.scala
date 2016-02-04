package util.adv

import com.google.inject.Inject
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.edge.{MEdgeInfo, MNodeEdges}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.14 10:14a
 * Description: Если все районы города покрыты размещением, то размещение должно перебрасываться и на весь город.
 * Здесь калькулятор карты экстра-ресиверов, который реализует этот принцип.
 *
 * 2016.feb.4: Удалить и этот модуль, когда будет произведён отказ от районов-городов.
 */
class AdvTownCoverageRcvrs @Inject() (
  mCommonDi     : ICommonDi
)
  extends AdvExtraRcvrsCalculator
  with PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Метод для проверки включенности модуля в рабочий процесс. */
  override def isEnabled: Boolean = configuration.getBoolean("adv.fwd.town.coverage.enabled") getOrElse true

  /**
   * Рассчет town-ресиверов на основе карты прямых (непосредственных) ресиверов карточки.
   *
   * @param allDirectRcvrs Карта непосредственных ресиверов.
   * @param producerIdOpt id продьюсера.
   * @return Фьючерс с картой extra-ресиверов.
   */
  override def calcForDirectRcvrs(allDirectRcvrs: Receivers_t, producerIdOpt: Option[String]): Future[Receivers_t] = {

    val allRcvrsFut = {
      val rcvrsIter = allDirectRcvrs
        .iterator
        .map(_._2.nodeId)
      mNodeCache.multiGet(rcvrsIter)
    }

    val districtTypeNames = AdnShownTypes.districtNames
    // Выбрать только ресиверы районов, сгруппировать по гео-родителям (городам).
    val townDistrictsRcvrsMapFut = for (districtNodes <- allRcvrsFut) yield {
      val filtered = districtNodes
        .iterator
        .filter { mnode =>
          mnode.extras.adn
            .flatMap( _.shownTypeIdOpt )
            .exists( districtTypeNames.contains )
        }
      groupByDirectGeoParents(filtered)
    }

    // Рисуем карту узлов городов: townAdnId -> townNode
    val townsMapFut = for {
      tdRcvrsMap <- townDistrictsRcvrsMapFut
      nodes      <- mNodeCache.multiGet( tdRcvrsMap.keysIterator )
    } yield {
       nodes.iterator
        .filter { _.extras.adn.flatMap(_.shownTypeIdOpt).contains(AdnShownTypes.TOWN.name) }
        .filter { _.extras.adn.exists(_.sinks contains AdnSinks.SINK_GEO) }
        .flatMap { node => node.id.map(_ -> node) }
        .toMap
    }

    // Карта реальных городов и их районов размещений, на которых есть размещения.
    val townDistrictIdsMapFut = for {
      tdMap     <- townDistrictsRcvrsMapFut
      tdIdsMap  = tdMap.mapValues { _.flatMap(_.id) }
      tMap      <- townsMapFut
    } yield {
      tdIdsMap.filterKeys { tMap.contains }
    }

    // Запустить поиск всех районов во всех найденных городах
    val townAll2DistrictIdsMapFut = for {
      townsMap <- townsMapFut
      res <- {
        // Для существенного снижения нагрузки на RAM используем запросы по городам.
        Future.traverse(townsMap.keysIterator) { townAdnId =>
          val sargs2 = new MNodeSearchDfltImpl {
            override def outEdges: Seq[ICriteria] = {
              val cr = Criteria(Seq(townAdnId), Seq(MPredicates.GeoParent.Direct))
              Seq(cr)
            }
            override def limit            = 70    // Наврядли в городе больше указанного кол-ва узлов. // TODO Брать число из другого места...
            override def shownTypeIds     = districtTypeNames
            override def onlyWithSinks    = Seq(AdnSinks.SINK_GEO)
          }
          for(allTownDistrictsIds <- MNode.dynSearchIds(sargs2)) yield {
            townAdnId -> allTownDistrictsIds.toSet
          }
        }
      }
    } yield {
      res.toMap
    }

    // Считаем текущие размерности затрагиваемых городов (общее кол-во районов в городе)
    val townAllDistrictsCountMapFut = for (tdisMap <- townAll2DistrictIdsMapFut) yield {
      tdisMap.mapValues(_.size)
    }

    // Карту размещений конвертим в карту townAdnId -> sls. Для этого нужна карта district -> town
    val resultFut = for {
      tdisMap           <- townDistrictIdsMapFut
      tdisCountMap      <- townAllDistrictsCountMapFut
    } yield {
      val eiter = tdisMap
        .iterator
        .flatMap { case (townId, districtIds) =>
          // сколько попаданий по уровню нужно, чтобы он прошел на уровень выше? Столько же, сколько и районов в этом городе.
          val townDistrictsCount = tdisCountMap(townId)
          districtIds
            .iterator
            // Раскрываем уровни отображения
            .flatMap { districtId =>
              allDirectRcvrs
                .valuesIterator
                .find { e =>
                  e.predicate == MPredicates.Receiver && e.nodeId == districtId
                }
                .iterator
                .flatMap( _.info.sls.iterator )
            }
            .toSeq
            // Считаем частоты уровней отображения
            .groupBy(identity)
            .iterator
            .map { case (ssl, ssls)  =>  ssl -> ssls.size }
            // фильтруем уровень по частоте
            .filter { case (ssl, sslFreq)  =>  sslFreq >= townDistrictsCount }
            // Возвращаем результат для уровней, которые покрывают весь город
            .map { case (ssl, sslFreq)  =>  townId -> ssl }
        }
        .toSeq
        .groupBy(_._1)
        .iterator
        .map { case (townId, sslsRaw)  =>
          MEdge(MPredicates.Receiver, townId, info = MEdgeInfo(
            sls = sslsRaw.map(_._2).toSet
          ))
        }
      MNodeEdges.edgesToMap1( eiter )
    }

    // Напечатать в логи результат сложной мыслительной работы.
    if (LOGGER.underlying.isDebugEnabled) {
      resultFut onSuccess { case resultRcvrs =>
        if (resultRcvrs.nonEmpty)
          debug("Town coverage detected: +" + resultRcvrs)
      }
    }
    resultFut
  }


  private def groupByDirectGeoParents(nodes: Iterator[MNode]): Map[String, Seq[MNode]] = {
    nodes
      .flatMap { mnode =>
        mnode.edges
          .withPredicateIterIds( MPredicates.GeoParent.Direct )
          .map { _ -> mnode }
      }
      .toSeq
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

}
