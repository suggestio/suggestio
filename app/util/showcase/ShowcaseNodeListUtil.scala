package util.showcase

import com.google.inject.{Singleton, Inject}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.geo.GeoDistanceQuery
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search.{MNodeSearchDfltImpl, MNodeSearch}
import io.suggest.ym.model.NodeGeoLevels
import org.elasticsearch.client.Client
import org.elasticsearch.search.sort.SortOrder
import play.api.Configuration
import play.api.i18n.Messages
import util.PlayMacroLogsImpl
import models._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 18:40
 * Description: Утиль для работы со списком узлов в выдаче: определение нод, построение дерева и т.д.
 */

@Singleton
class ShowcaseNodeListUtil @Inject() (
  mNodeCache              : MAdnNodeCache,
  configuration           : Configuration,
  implicit val ec         : ExecutionContext,
  implicit val esClient   : Client
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  /** Показывать все города в выдаче или только текущий? */
  val SHOW_ALL_TOWNS: Boolean = configuration.getBoolean("showcase.nodes.towns.show.all") getOrElse false

  /** Если включён вывод списка городов, то надо определить макс.длину этого списка. */
  val MAX_TOWNS: Int = configuration.getInt("showcase.nodes.towns.max") getOrElse 10

  /** Использовать сортировку не по имени, а по расстоянию до узлов. */
  val DISTANCE_SORT: Boolean = configuration.getBoolean("showcase.nodes.sort.distance") getOrElse false


  /**
   * Запуск детектора текущей ноды и её геоуровня. Асинхронно возвращает (lvl, node) или экзепшен.
   * Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
   * @param geoMode Текущий георежим.
   * @param gsiOptFut Фьючерс с геоинфой.
   * @param searchF Используемая для поиска, которая возвращает список результатов произвольного типа.
   * @return Фьючерс с результатом детектирования. Если не удалось, то будет exception.
   */
  def detectCurrentNodeUsing[T](geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]])
                               (searchF: MNodeSearch => Future[Seq[T]]): Future[(NodeGeoLevel, T)] = {
    val detectLevels = geoMode.nodeDetectLevels.iterator.zipWithIndex
    Future.traverse(detectLevels) { case (lvl, prio) =>
      gsiOptFut.map { gsiOpt =>
        new MNodeSearchDfltImpl {
          override def limit = 1
          override def withGeoDistanceSort: Option[GeoPoint] = {
            // 2015.jun.30 Стараемся всегда искать с учетом всех возможных опорных геоточек.
            geoMode.exactGeodata
              .orElse { gsiOpt.map(_.geoPoint) }
          }

          override def gsLevels = Seq(lvl)

          override def gsShapes: Seq[GeoDistanceQuery] = {
            gsiOpt
              .map { _.geoDistanceQuery }
              .toSeq
          }
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
          .map {
            case (_lvl, _nodes, _prio) =>
              (_lvl, _nodes.head, _prio)
          }
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
    detectCurrentNodeUsing(geoMode, gsiOptFut)(MNode.dynSearch)
      .map { case (lvl, node) => GeoDetectResult(lvl, node) }
  }

  /**
   * Последовательное объединение функционала двух методов детектирования.
   * @param geoMode Режим геолокации.
   * @param gsiOptFut Результат получаения геоинфы по реквесту.
   * @param currAdnIdOpt Возможные данные по текущему узлу.
   * @return Опциональный результат с узлом, на котором сейчас находимся.
   */
  private def detectOrGuessCurrentNode(geoMode: GeoMode, gsiOptFut: Future[Option[GeoSearchInfo]],
                                       currAdnIdOpt: Option[String]) : Future[MNode] = {
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
                                   (detectFut: Future[GeoDetectResult]): Future[MNode] = {
    detectFut
      .map(_.node)
      .recoverWith {
        case ex: NoSuchElementException =>
          // Запускаем чтение из кеша уже известного узла.
          mNodeCache.maybeGetByIdCached(currAdnIdOpt)
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
            MNode.dynSearch(sargs)
              .map(_.head)
          }
      }
  }

  /** Краткая реализация для поиска узлов по id и отображаемому типу. */
  private case class NodeSearchByIdShownType(
    override val withIds: Seq[String],
    override val shownTypeIds: Seq[String]
  )
    extends NodeDetectArgsT

  /**
   * Найти узел города для узла.
   * @param mnode Текущий ADN-узел.
   * @return Фьючерс с найденным городом или текущий узел, если он и есть город.
   *         NoSuchElementException если нода болтается в воздухе.
   */
  def getTownOfNode(mnode: MNode): Future[MNode] = {
    val ast: AdnShownType = mnode.extras.adn
      .flatMap(_.shownTypeIdOpt)
      .flatMap(AdnShownTypes.maybeWithName)
      .getOrElse( AdnShownTypes.default )
    if (ast == AdnShownTypes.TOWN) {
      Future successful mnode
    } else {
      val allParentIds = mnode.edges
        .withPredicateIterIds( MPredicates.GeoParent )
        .toSeq
      val sargs1 = NodeSearchByIdShownType(allParentIds, shownTypeIds = Seq(AdnShownTypes.TOWN.name))
      MNode.dynSearch( sargs1 )
        .map(_.head)
        // 2015.jun.18 Была выявлена проблема в head, когда город отсутствует. Пытаемся найти район, а из него город уже.
        .recoverWith { case ex: NoSuchElementException if ast.isBuilding =>
          error("getTownOfNode() geo-inconsistent linked node: id=" + mnode.id, ex)
          val districtTypeNames = AdnShownTypes.districtNames
          val sargs2 = NodeSearchByIdShownType(allParentIds, shownTypeIds = districtTypeNames)
          MNode.dynSearch(sargs2)
            .map { _.head }
            .flatMap { districtNode =>
              getTownOfNode(districtNode)
            }
        }
    }
  }

  def getTownLayerOfNode(node: MNode)(implicit lang: Messages): Future[GeoNodesLayer] = {
    getTownOfNode(node)
      .map { town2layer(_) }
  }

  def town2layer(townNode: MNode, expanded: Boolean = false) = {
    GeoNodesLayer( Seq(townNode), NodeGeoLevels.NGL_TOWN )
  }


  /** Выдать все города */
  def allTowns(currGeoPoint: Option[GeoPoint]): Future[Seq[MNode]] = {
    val sargs = new NodeDetectArgsT {
      override def shownTypeIds = Seq(AdnShownTypes.TOWN.name)
      override def withNameSort = if (currGeoPoint.isEmpty) Some(SortOrder.ASC) else None
      override def limit        = MAX_TOWNS
      override def withGeoDistanceSort = currGeoPoint
    }
    MNode.dynSearch(sargs)
  }

  /** Обернуть список городов в гео-слой. */
  def townsToLayer(townNodes: Seq[MNode], expanded: Boolean)(implicit lang: Messages): GeoNodesLayer = {
    if (townNodes.isEmpty) {
      GeoNodesLayer(Seq.empty, NodeGeoLevels.NGL_TOWN, expanded = expanded)
    } else if (townNodes.size == 1) {
      town2layer(townNodes.head, expanded)
    } else {
      GeoNodesLayer(
        nodes     = townNodes,
        ngl       = NodeGeoLevels.NGL_TOWN,
        nameOpt   = Some( Messages(NodeGeoLevels.NGL_TOWN.l10nPluralShort)),
        expanded  = expanded
      )
    }
  }

  /**
   * Узнаём уровень, на котором находится текущая нода.
   * @param detectFut результат detectCurrentNode().
   * @param currNodeFut результат detectRecoverGuessCurrentNode().
   * @return Фьючерс, по формату совпадающий с detectRecoverGuessCurrentNode().
   */
  def nextNodeWithLvlOptFut(detectFut: Future[GeoDetectResult], currNodeFut: Future[MNode]): Future[GeoDetectResult] = {
    detectFut
      .recoverWith {
        case ex: NoSuchElementException =>
          currNodeFut flatMap { nextNode =>
            MAdnNodeGeo.findIndexedPtrsForNode(nextNode.id.get, maxResults = 1)
              .map { vs =>
                // 2014.sep.29: возник экзепшен на узле, который не имел нормальных гео-настроек, но был выбран вручную в выдаче.
                val glevel: NodeGeoLevel = vs.headOption.map(_.glevel).orElse {
                  nextNode.extras.adn
                    .flatMap(_.shownTypeIdOpt)
                    .flatMap(AdnShownTypes.maybeWithName)
                    .flatMap(_.ngls.headOption)
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
  def getDistrictsForTown(townNodeId: String, gravity: Option[GeoPoint]): Future[Seq[MNode]] = {
    val sargs = new SmNodesSearchArgsT {
      override def limit = 20
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(Seq(townNodeId), Seq(MPredicates.GeoParent.Direct))
        Seq(cr)
      }
      override def shownTypeIds = AdnShownTypes.districtNames
      override def withNameSort = if (gravity.isEmpty) Some(SortOrder.ASC) else None
      override def withGeoDistanceSort = gravity
    }
    MNode.dynSearch(sargs)
  }

  def getDistrictsLayerForTown(townNode: MNode, gravity: Option[GeoPoint], expanded: Boolean = false)
                              (implicit lang: Messages): Future[GeoNodesLayer] = {
    val townNodeId = townNode.id.get
    getDistrictsForTown(townNodeId, gravity)
      .map { districtNodes =>
        // 2014.sep.30: В Москве у нас "округа", а не "районы". Да и возможны иные вариации. Определяем код названия по найденным нодам.
        val nameL10n = districtNodes
          .headOption
          .flatMap(_.extras.adn)
          .flatMap(_.shownTypeIdOpt)
          .flatMap(AdnShownTypes.maybeWithName)
          .fold(NodeGeoLevels.NGL_TOWN_DISTRICT.l10nPluralShort)(_.pluralNoTown)
        GeoNodesLayer(
          nodes   = districtNodes,
          ngl     = NodeGeoLevels.NGL_TOWN_DISTRICT,
          nameOpt = Some( Messages(nameL10n) ),
          expanded = expanded
        )
      }
  }


  /**
   * Собрать здания в рамках района.
   * @param districtAdnId id района.
   * @return Список узлов на раёне в алфавитном порядке.
   */
  def getBuildingsOfDistrict(districtAdnId: String, gravity: Option[GeoPoint]): Future[Seq[MNode]] = {
    val sargs = new SmNodesSearchArgsT {
      override def limit = 30
      override def withAdnRights = Seq(AdnRights.RECEIVER)
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(Seq(districtAdnId), Seq(MPredicates.GeoParent.Direct))
        Seq(cr)
      }
      override def withGeoDistanceSort = gravity
      override def withNameSort = if (gravity.isEmpty)  Some(SortOrder.ASC)  else  None
    }
    MNode.dynSearch(sargs)
  }

  /**
   * Получение нод и сборка слоёв для зданий района.
   * @param districtAdnId id узла района.
   * @param expandOnNode Если слой содержит узел с указанным id, то развернуть выставить expanded = true.
   * @return Фьючерс со списком слоёв с узлами.
   */
  def getBuildingsLayersOfDistrict(districtAdnId: String, gravity: Option[GeoPoint], expandOnNode: Option[String] = None)
                                  (implicit lang: Messages): Future[List[GeoNodesLayer]] = {
    getBuildingsOfDistrict(districtAdnId, gravity)
      .map { nodes =>
        nodes
          .iterator
          .flatMap { mnode =>
            mnode.extras.adn
              .flatMap( _.shownTypeIdOpt )
              .flatMap( AdnShownTypes.maybeWithName )
              .map { _ -> mnode }
          }
          .toSeq
          .groupBy(_._1)
          .iterator
          .map { case (sti, layNodesSti) =>
            sti -> layNodesSti.map(_._2)
          }
          .map { case (sti, layNodes) =>
            val ast: AdnShownType = sti
            val lsSorted = layNodes.sortBy(_.meta.basic.nameShort)
            val expanded = expandOnNode.isDefined && {
              val currAdnId = expandOnNode.get
              layNodes.iterator
                .flatMap(_.id)
                .contains(currAdnId)
            }
            GeoNodesLayer(lsSorted, NodeGeoLevels.NGL_BUILDING, Some(Messages(ast.pluralNoTown)), expanded = expanded)
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
  def collectLayers(geoMode: Option[GeoMode], currNode: MNode, currNodeLayer: NodeGeoLevel)
                   (implicit lang: Messages): Future[Seq[GeoNodesLayer]] = {
    // Задаём опорные геоточки для гео-сортировки и гео-поиска.
    val (gravity0, gravity1) = if (DISTANCE_SORT && geoMode.isDefined) {
      val gm = geoMode.get
      val g0 = gm.exactGeodata
      val g1 = g0.orElse(currNode.geo.point)
      g0 -> g1
    } else {
      None -> None
    }
    // В зависимости от ситуации, строим слои по разным технологиям.
    // На scala < 2.11.2 тут вылетает warning: match may not be exhausive. См. https://issues.scala-lang.org/browse/SI-8708
    currNodeLayer match {
      // Это -- город.
      case NodeGeoLevels.NGL_TOWN =>
        val districtsLayerFut = getDistrictsLayerForTown(currNode, gravity0)
        // 2014.sep.25: Нужна возможность выдавать другие города. Это должно быть отлючаемо.
        val townsLayerFut: Future[GeoNodesLayer] = if (SHOW_ALL_TOWNS) {
          allTowns(gravity1) map { townNodes =>
            townsToLayer(townNodes, expanded = true)
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
        val townFut = getTownOfNode(currNode)
        val buildingsFut = getBuildingsLayersOfDistrict(currNode.id.get, gravity1)
        val districtsOptFut: Future[Option[GeoNodesLayer]] = {
          townFut flatMap { townNode =>
            val directParentOpt = {
              currNode.edges
                .withPredicateIterIds( MPredicates.GeoParent.Direct )
                .collectFirst { case x => x }
            }
            FutureUtil.optFut2futOpt(directParentOpt) { dparent =>
              getDistrictsLayerForTown(townNode, gravity0, expanded = true)
                .map( Some.apply )
            }
          }
        }
        val townLayerFut = townFut.map { town2layer(_) }
        for {
          townLayer           <- townLayerFut
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
        // Если нет текущей геолокации, то можно использовать геолокацию зданию. Или не стоит так делать?
        val districtsLayerFut = townFut flatMap { townNode =>
          getDistrictsLayerForTown(townNode, gravity1)
        }
        val townLayerFut = townFut.map { town2layer(_) }
        val buildingsLayersFut = {
          currNode.edges
            .withPredicateIterIds( MPredicates.GeoParent.Direct )
            .collectFirst { case x => x }
            .fold(Future successful List.empty[GeoNodesLayer]) { currDistrictId =>
              getBuildingsLayersOfDistrict(currDistrictId, gravity1, expandOnNode = currNode.id)
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
sealed class SmNodesSearchArgsCommonT extends MNodeSearchDfltImpl {
  override def testNode = Some(false)
  override def isEnabled = Some(true)
}

/** В рамках списка узлов выдачи всегда НЕ нужны отключённые и тестовые узлы. */
sealed class SmNodesSearchArgsT extends SmNodesSearchArgsCommonT {
  override def showInScNodeList = Some(true)
}


/** При детектирования текущего узла происходит поиск единственного продакшен-ресивера.
  * Тут -- common-аргументы, задающие это поведение при поиске узлов. */
sealed class NodeDetectArgsT extends SmNodesSearchArgsCommonT {
  override def withAdnRights = Seq(AdnRights.RECEIVER)
  override def limit = 1
  override def offset = 0
}
