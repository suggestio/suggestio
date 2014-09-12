package util.adv

import io.suggest.ym.model.common.AdnSinks
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import play.api.Play.{configuration, current}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import scala.concurrent.Future
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.14 10:14a
 * Description: Если все районы города покрыты размещением, то размещение должно перебрасываться и на весь город.
 * Здесь калькулятор карты экстра-ресиверов, который реализует этот принцип. 
 */
object AdvTownCoverageRcvrs extends AdvExtraRcvrsCalculator {

  /** Метод для проверки включенности модуля в рабочий процесс. */
  override def isEnabled: Boolean = configuration.getBoolean("adv.fwd.town.coverage.enabled") getOrElse true

  /**
   * Рассчет town-ресиверов на основе карты прямых (непосредственных) ресиверов карточки.
   * @param allDirectRcvrs Карта непосредственных ресиверов.
   * @param producerId id продьюсера.
   * @return Фьючерс с картой extra-ресиверов.
   */
  override def calcForDirectRcvrs(allDirectRcvrs: Receivers_t, producerId: String): Future[Receivers_t] = {
    val allRcvrsFut = {
      val rcvrsIter = allDirectRcvrs
        .iterator
        .map(_._2.receiverId)
      MAdnNodeCache.multiGet(rcvrsIter)
    }
    // Выбрать только ресиверы районов, сгруппировать по гео-родителям (городам).
    val townDistrictsRcvrsMapFut = allRcvrsFut map { districtNodes =>
      val filtered = districtNodes
        .iterator
        .filter { _.adn.shownTypeId  ==  AdnShownTypes.TOWN_DISTRICT.name }
      groupByDirectGeoParents(filtered)
    }
    // Рисуем карту узлов городов: townAdnId -> townNode
    val townsMapFut = townDistrictsRcvrsMapFut.flatMap { tdRcvrsMap =>
      MAdnNodeCache.multiGet( tdRcvrsMap.keysIterator )
    } map {
       _.iterator
        .filter { _.adn.shownTypeId  ==  AdnShownTypes.TOWN.name }
        .filter { _.adn.hasGeoSink }
        .flatMap { node => node.id.map(_ -> node) }
        .toMap
    }
    // Карта реальных городов и их районов размещений, на которых есть размещения.
    val townDistrictIdsMapFut = townDistrictsRcvrsMapFut flatMap { tdMap =>
      val tdIdsMap = tdMap.mapValues { _.flatMap(_.id) }
      townsMapFut map { tMap =>
        tdIdsMap.filterKeys { tMap.contains }
      }
    }
    // Запустить поиск всех районов во всех найденных городах
    val townAll2DistrictIdsMapFut = townsMapFut flatMap { townsMap =>
      // Для существенного снижения нагрузки на RAM используем запросы по городам.
      Future.traverse(townsMap.keysIterator) { townAdnId =>
        val sargs = new AdnNodesSearchArgs {
          override def withDirectGeoParents = Seq(townAdnId)
          override def maxResults           = 70    // Наврядли в городе больше указанного кол-ва узлов. // TODO Брать число из другого места...
          override def shownTypeIds         = Seq(AdnShownTypes.TOWN_DISTRICT.name)
          override def onlyWithSinks        = Seq(AdnSinks.SINK_GEO)
        }
        MAdnNode.dynSearchIds(sargs) map { allTownDistrictsIds =>
          townAdnId -> allTownDistrictsIds.toSet
        }
      } map { _.toMap }
    }
    // Считаем текущие размерности затрагиваемых городов (общее кол-во районов в городе)
    val townAllDistrictsCountMapFut = townAll2DistrictIdsMapFut map { tdisMap =>
      tdisMap.mapValues(_.size)
    }
    // Карту размещений конвертим в карту townAdnId -> sls. Для этого нужна карта district -> town
    for {
      tdisMap           <- townDistrictIdsMapFut
      tdisCountMap      <- townAllDistrictsCountMapFut
    } yield {
      tdisMap
        .iterator
        .flatMap { case (townId, districtIds) =>
          // сколько попаданий по уровню нужно, чтобы он прошел на уровень выше? Столько же, сколько и районов в этом городе.
          val townDistrictsCount = tdisCountMap(townId)
          districtIds
            .iterator
            // Раскрываем уровни отображения
            .flatMap { districtId =>
              allDirectRcvrs.get(districtId)
                .fold [Iterator[SinkShowLevel]] (Iterator.empty) { _.sls.iterator }
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
        .map { case (townId, sslsRaw)  =>  townId -> AdReceiverInfo(townId, sslsRaw.map(_._2).toSet) }
        .toMap
    }
  }


  private def groupByDirectGeoParents(nodes: Iterator[MAdnNode]): Map[String, Seq[MAdnNode]] = {
    nodes
      .flatMap { node => node.geo.directParentIds.map(_ -> node) }
      .toSeq
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

}
