package util.showcase


import io.suggest.model.geo.GeoShapeQueryData
import io.suggest.ym.model.common.IBlockMeta
import models.{BlockConf, MMartCategory, _}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.RequestHeader
import util.SiowebEsUtil.client
import util.blocks.BlocksConf
import AdnShownTypes.adnInfo2val

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 18:40
 * Description:
 */

object ShowcaseNodeListUtil {

  /**
   * Запуск детектора текущей ноды и её геоуровня. Асинхронно возвращает (lvl, node) или экзепшен.
   * Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
   * @param geoMode Текущий георежим.
   * @return Фьючерс с результатом детектирования. Если не удалось, то будет exception.
   */
  def detectCurrentNode(geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]]): Future[GeoDetectResult] = {
    val detectLevels = geoMode.nodeDetectLevels.iterator.zipWithIndex
    Future.traverse(detectLevels) { case (lvl, prio) =>
      gsiOptFut.map { gsiOpt =>
        new NodeDetectArgsT {
          override val geoDistance = gsiOpt.map { gsi => GeoShapeQueryData(gsi.geoDistanceQuery, lvl) }
          override val withGeoDistanceSort = gsiOpt.map { _.geoPoint }
        }
      } flatMap { sargs =>
        MAdnNode.dynSearch(sargs)
      } map {
        (lvl, _, prio)
      }
    } map { results =>
      val filtered = results
        .filter(_._2.nonEmpty)
      if (filtered.nonEmpty) {
        val (lvl, node, _) = filtered
          .map { case (_lvl, _nodes, _prio) => (_lvl, _nodes.head, _prio) }
          .minBy(_._3)
        Some(GeoDetectResult(lvl, node))
      } else {
        None
      }
    } filter {
      _.nonEmpty
    } map {
      _.get
    }
  }


  /**
   * Последовательное объединение функционала двух методов детектирования.
   * @param geoMode Режим геолокации.
   * @param gsiOptFut Результат получаения геоинфы по реквесту.
   * @param currAdnIdOpt Возможные данные по текущему узлу.
   * @return Опциональный результат с узлом, на котором сейчас находимся.
   */
  private def detectOrGuessCurrentNode(geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]], currAdnIdOpt: Option[String]
                               ) : Future[MAdnNode] = {
    detectRecoverGuessCurrentNode(gsiOptFut, currAdnIdOpt) {
      detectCurrentNode(geoMode, gsiOptFut)
    }
  }


  /**
   * Всегда можно найти узел, к которому отнести выдачу.
   * @param gsiOptFut Фьючерс с данными по географии реквеста.
   * @param currAdnIdOpt Возможная инфа об узле.
   * @param detectFut Запущенный процесс детектирования узла.
   * @return Фьючерс с узлом. Если в базе нет продакшен-ресиверов вообще, то будет экзепшен.
   */
  def detectRecoverGuessCurrentNode(gsiOptFut: Future[Option[GeoSearchInfo]], currAdnIdOpt: Option[String])
                                   (detectFut: Future[GeoDetectResult]): Future[MAdnNode] = {
    detectFut
      .map(_.node)
      .recoverWith {
        case ex: NoSuchElementException =>
          // Запускаем чтение из кеша уже известного узла.
          MAdnNodeCache.maybeGetByIdCached(currAdnIdOpt)
            .filter(_.nonEmpty)
            .map(_.get)

      }
      .recoverWith {
        // Нет ноды. Вероятно, человек где-то в провинции. Нужно тупо найти ближайший город.
        case ex: NoSuchElementException =>
          gsiOptFut flatMap { gsiOpt =>
            val sargs = new NodeDetectArgsT {
              override val withGeoDistanceSort = gsiOpt.map(_.geoPoint)
              override val shownTypeIds = Seq(AdnShownTypes.TOWN.name)
            }
            MAdnNode.dynSearch(sargs)
              .map(_.head)
          }
      }
  }


  /**
   * Найти узел города для узла.
   * @param node Текущий ADN-узел.
   * @return Фьючерс с найденным городом или текущий узел, если он и есть город.
   *         NoSuchElementException если нода болтается в воздухе.
   */
  def getTownOfNode(node: MAdnNode): Future[MAdnNode] = {
    val ast: AdnShownType = node.adn
    if (ast == AdnShownTypes.TOWN) {
      Future successful node
    } else {
      val sargs = new NodeDetectArgsT {
        override val withIds = node.geo.allParentIds.toSeq
        override val shownTypeIds = Seq(AdnShownTypes.TOWN.name)
      }
      MAdnNode.dynSearch(sargs)
        .map(_.head)
    }
  }

  def getTownLayerOfNode(node: MAdnNode): Future[GeoNodesLayer] = {
    getTownOfNode(node)
      .map { townNode => GeoNodesLayer(Seq(townNode), NodeGeoLevels.NGL_TOWN) }
  }


  /**
   * Узнаём уровень, на котором находится текущая нода.
   * @param detectFut результат detectCurrentNode().
   * @param currNodeFut результат detectRecoverGuessCurrentNode().
   * @return Фьючерс, по формату совпадающий с detectRecoverGuessCurrentNode().
   */
  def nextNodeWithLvlOptFut(detectFut: Future[GeoDetectResult], currNodeFut: Future[MAdnNode]): Future[GeoDetectResult] = {
    detectFut
      .recoverWith {
        case ex: NoSuchElementException =>
          currNodeFut flatMap { nextNode =>
            MAdnNodeGeo.findIndexedPtrsForNode(nextNode.id.get, maxResults = 1)
              .map { vs => GeoDetectResult(vs.head.glevel, nextNode) } }
      }
  }


  /**
   * Найти районы для города.
   * @param townNode узел города.
   * @return Список узлов-районов.
   */
  def getDistrictsForTown(townNode: MAdnNode): Future[Seq[MAdnNode]] = {
    val sargs = new AdnNodesSearchArgs {
      override def maxResults = 20
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def withDirectGeoParents: Seq[String] = Seq(townNode.id.get)
      override def testNode = Some(false)
      override def shownTypeIds = Seq(AdnShownTypes.TOWN_DISTRICT.name)
      override def withNameSort = true
      override def isEnabled = Some(true)
    }
    MAdnNode.dynSearch(sargs)
  }

  def getDistrictsLayerForTown(townNode: MAdnNode): Future[GeoNodesLayer] = {
    getDistrictsForTown(townNode)
      .map { districtNodes  =>  GeoNodesLayer(districtNodes, NodeGeoLevels.NGL_TOWN_DISTRICT) }
  }


  def getBuildingsOfDistrict(districtAdnId: String): Future[Seq[MAdnNode]] = {
    val sargs = new AdnNodesSearchArgs {
      override def maxResults = 30
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def withDirectGeoParents = Seq(districtAdnId)
      override def testNode = Some(false)
      override def withNameSort = true
      override def isEnabled = Some(true)
    }
    MAdnNode.dynSearch(sargs)
  }

}


/** При детектирования текущего узла происходит поиск единственного продакшен-ресивера.
  * Тут -- common-аргументы, задающие это поведение при поиске узлов. */
sealed trait NodeDetectArgsT extends AdnNodesSearchArgs {
  override def withAdnRights = Seq(AdnRights.RECEIVER)
  override def maxResults = 1
  override def offset = 0
  override def testNode = Some(false)
  override def isEnabled = Some(true)
}
