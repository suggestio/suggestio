package util.showcase

import io.suggest.model.geo.GeoShapeQueryData
import io.suggest.ym.model.NodeGeoLevels
import play.api.i18n.{Messages, Lang}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}
import util.SiowebEsUtil.client
import models._
import AdnShownTypes.adnInfo2val

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 18:40
 * Description:
 */

object ShowcaseNodeListUtil {

  /** Показывать все города в выдаче или только текущий? */
  val SHOW_ALL_TOWNS: Boolean = configuration.getBoolean("showcase.nodes.towns.show.all") getOrElse false

  /** Если включён вывод списка городов, то надо определить макс.длину этого списка. */
  val MAX_TOWNS: Int = configuration.getInt("showcase.nodes.towns.max") getOrElse 10


  /**
   * Запуск детектора текущей ноды и её геоуровня. Асинхронно возвращает (lvl, node) или экзепшен.
   * Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
   * @param geoMode Текущий георежим.
   * @param gsiOptFut Фьючерс с геоинфой.
   * @param searchF Используемая для поиска, которая возвращает список результатов произвольного типа.
   * @return Фьючерс с результатом детектирования. Если не удалось, то будет exception.
   */
  def detectCurrentNodeUsing[T](geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]])
                               (searchF: AdnNodesSearchArgsT => Future[Seq[T]]): Future[(NodeGeoLevel, T)] = {
    val detectLevels = geoMode.nodeDetectLevels.iterator.zipWithIndex
    Future.traverse(detectLevels) { case (lvl, prio) =>
      gsiOptFut.map { gsiOpt =>
        new NodeDetectArgsT {
          override def geoDistance = gsiOpt.map { gsi => GeoShapeQueryData(gsi.geoDistanceQuery, lvl) }
          override def withGeoDistanceSort = geoMode.exactGeodata  // TODO Сделать обязательным м.б.?
          override def maxResults = 1
        }
      } flatMap { sargs =>
        searchF(sargs)
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
        Some((lvl, node))
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
   * Запуск поиска экземпляра текущей ноды. По сути враппер над detectCurrentNodeUsing().
   * @param geoMode Текущий geo-режим работы.
   * @param gsiOptFut Фоново-собираемые данные о географии.
   * @return Фьючерс с GeoDetectResult.
   */
  def detectCurrentNode(geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]]): Future[GeoDetectResult] = {
    detectCurrentNodeUsing(geoMode, gsiOptFut)(MAdnNode.dynSearch)
      .map { case (lvl, node) => GeoDetectResult(lvl, node) }
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

  def getTownLayerOfNode(node: MAdnNode)(implicit lang: Lang): Future[GeoNodesLayer] = {
    getTownOfNode(node)
      .map(town2layer)
  }

  def town2layer(townNode: MAdnNode) = {
    GeoNodesLayer( Seq(townNode) )
  }


  /** Выдать все города,  */
  def allTowns(currGeoPoint: Option[GeoPoint]): Future[Seq[MAdnNode]] = {
    val sargs = new NodeDetectArgsT {
      override def shownTypeIds = Seq(AdnShownTypes.TOWN.name)
      override def withGeoDistanceSort = currGeoPoint
      override def withNameSort = currGeoPoint.isEmpty
      override def maxResults = MAX_TOWNS
    }
    MAdnNode.dynSearch(sargs)
  }

  /** Обернуть список городов в гео-слой. */
  def townsToLayer(townNodes: Seq[MAdnNode])(implicit lang: Lang): GeoNodesLayer = {
    if (townNodes.isEmpty) {
      GeoNodesLayer(Seq.empty)
    } else if (townNodes.tail.isEmpty) {
      town2layer(townNodes.head)
    } else {
      GeoNodesLayer(
        nodes = townNodes,
        nameOpt = Some( Messages(NodeGeoLevels.NGL_TOWN.l10nPluralShort)) )
    }
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
              .map { vs =>
                // 2014.sep.29: возник экзепшен на узле, который не имел нормальных гео-настроек, но был выбран вручную в выдаче.
                val glevel: NodeGeoLevel = vs.headOption.map(_.glevel).orElse {
                  AdnShownTypes.maybeWithName(nextNode.adn.shownTypeId).flatMap(_.ngls.headOption)
                } getOrElse {
                  NodeGeoLevels.default    // should never happen
                }
                GeoDetectResult(glevel, nextNode)
            }
          }
      }
  }


  /**
   * Найти районы для города.
   * @param townNodeId id узла-города.
   * @return Список узлов-районов.
   */
  def getDistrictsForTown(townNodeId: String): Future[Seq[MAdnNode]] = {
    val sargs = new SmNodesSearchArgsT {
      override def maxResults = 20
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def withDirectGeoParents: Seq[String] = Seq(townNodeId)
      override def shownTypeIds = Seq(AdnShownTypes.TOWN_DISTRICT.name)
      override def withNameSort = true
    }
    MAdnNode.dynSearch(sargs)
  }

  def getDistrictsLayerForTown(townNodeId: String)(implicit lang: Lang): Future[GeoNodesLayer] = {
    getDistrictsForTown(townNodeId)
      .map { districtNodes =>
        GeoNodesLayer(
          nodes = districtNodes,
          nameOpt = Some( Messages(NodeGeoLevels.NGL_TOWN_DISTRICT.l10nPluralShort) )
        )
      }
  }


  /**
   * Собрать здания в рамках района.
   * @param districtAdnId id района.
   * @return Список узлов на раёне в алфавитном порядке.
   */
  def getBuildingsOfDistrict(districtAdnId: String): Future[Seq[MAdnNode]] = {
    val sargs = new SmNodesSearchArgsT {
      override def maxResults = 30
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def withDirectGeoParents = Seq(districtAdnId)
      // Сортировать по имени не требуется, т.к. тут будет группировка.
    }
    MAdnNode.dynSearch(sargs)
  }

  /**
   * Получение нод и сборка слоёв для зданий района.
   * @param districtAdnId id узла района.
   * @return Фьючерс со списком слоёв с узлами.
   */
  def getBuildingsLayersOfDistrict(districtAdnId: String)(implicit lang: Lang): Future[List[GeoNodesLayer]] = {
    getBuildingsOfDistrict(districtAdnId)
      .map { nodes =>
        nodes.groupBy(_.adn.shownTypeId)
          .iterator
          .map { case (sti, layNodes) =>
            val ast: AdnShownType = sti
            val lsSorted = layNodes.sortBy(_.meta.nameShort)
            GeoNodesLayer(lsSorted, Some(Messages(ast.pluralNoTown)))
          }
          .toList
          .sortBy(_.nameOpt.getOrElse(""))
      }
  }


  /**
   * Сбор стопки слоёв в одну кучу.
   * @param geoMode Текущий режим геолокации.
   * @param currNode Текущий узел.
   * @param currNodeLayer Уровень, на котором находится текущий узел.
   * @return Фьючерс со слоями в порядке рендера (город внизу).
   */
  def collectLayers(geoMode: GeoMode, currNode: MAdnNode, currNodeLayer: NodeGeoLevel)(implicit lang: Lang): Future[Seq[GeoNodesLayer]] = {
    currNodeLayer match {
      // Это -- город.
      case NodeGeoLevels.NGL_TOWN =>
        val districtsLayerFut = getDistrictsLayerForTown(currNode.id.get)
        // 2014.sep.25: Нужно выдавать другие города в целях отладки. Это должно быть отлючаемо.
        val townsLayerFut: Future[GeoNodesLayer] = if (SHOW_ALL_TOWNS) {
          val gpOpt = geoMode.exactGeodata.orElse(currNode.geo.point)
          allTowns(gpOpt) map { townNodes =>
            townsToLayer(townNodes)
          }
        } else {
          Future successful town2layer(currNode)
        }
        for {
          districtsLayer <- districtsLayerFut
          townsLayer     <- townsLayerFut
        } yield {
          Seq(districtsLayer, townsLayer)
            .filter(_.nodes.nonEmpty)
        }

      // Юзер сейчас находится на уровне района. Нужно найти узлы в этом районе, город и остальные районы.
      case NodeGeoLevels.NGL_TOWN_DISTRICT =>
        val districtsOptFut: Future[Option[GeoNodesLayer]] = {
          currNode.geo.directParentIds.headOption match {
            case Some(dparent)  => getDistrictsLayerForTown(dparent).map(Some.apply)
            case None           => Future successful None
          }
        }
        val buildingsFut = getBuildingsLayersOfDistrict(currNode.id.get)
        for {
          townLayer           <- getTownLayerOfNode(currNode)
          districtsLayerOpt   <- districtsOptFut
          buildingsLayers     <- buildingsFut
        } yield {
          var acc = buildingsLayers
          if (districtsLayerOpt.isDefined)
            acc ::= districtsLayerOpt.get
          (townLayer :: acc)
            .filter(_.nodes.nonEmpty)
            .reverse
        }

      // Юзер гуляет на уровне зданий района. Нужно отобразить другие здания района, список районов, город.
      case NodeGeoLevels.NGL_BUILDING =>
        val townFut = getTownOfNode(currNode)
        val districtsLayerFut = townFut flatMap { townNode =>
          getDistrictsLayerForTown(townNode.id.get)
        }
        val townLayerFut = townFut.map(town2layer)
        val buildingsLayersFut = {
          currNode.geo.directParentIds.headOption match {
            case Some(currDistrictId) => getBuildingsLayersOfDistrict(currDistrictId)
            case None                 => Future successful Nil
          }
        }
        for {
          townLayer         <- townLayerFut
          districtsLayer    <- districtsLayerFut
          buildingsLayers   <- buildingsLayersFut
        } yield {
          (townLayer :: districtsLayer :: buildingsLayers)
            .filter(_.nodes.nonEmpty)
            .reverse
        }
    }
  }

}


/** общие аргументов для обоих целей. */
sealed trait SmNodesSearchArgsCommonT extends AdnNodesSearchArgs {
  override def testNode = Some(false)
  override def isEnabled = Some(true)
}

/** В рамках списка узлов выдачи всегда НЕ нужны отключённые и тестовые узлы. */
sealed trait SmNodesSearchArgsT extends SmNodesSearchArgsCommonT {
  override def showInScNodeList = Some(true)
}


/** При детектирования текущего узла происходит поиск единственного продакшен-ресивера.
  * Тут -- common-аргументы, задающие это поведение при поиске узлов. */
sealed trait NodeDetectArgsT extends SmNodesSearchArgsCommonT {
  override def withAdnRights = Seq(AdnRights.RECEIVER)
  override def maxResults = 1
  override def offset = 0
}
