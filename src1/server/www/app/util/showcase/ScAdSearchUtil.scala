package util.showcase

import javax.inject.Inject
import io.suggest.ble.{BeaconUtil, MUidBeacon}
import io.suggest.es.model.{EsModel, IMust, MEsInnerHitsInfo, MEsNestedSearch}
import io.suggest.es.search.{MRandomSortData, MSubSearch}
import io.suggest.geo.{GeoShapeJvm, MNodeGeoLevels, PointGs}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.edge.search.{Criteria, GsCriteria}
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sc.sc3.MScQs
import io.suggest.util.logs.MacroLogsImpl
import play.api.inject.Injector
import util.ble.BleUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:18
  * Description: Утиль для поиска карточек в рамках выдачи.
  * Появлась в ходе распиливании исторически-запутанной модели models.AdSearch,
  * т.к. забинденные qs-данные нужно было приводить к MNodeSearch, что может потребовать исполнения
  * асинхронного кода (например, в случае маячков).
  */
final class ScAdSearchUtil @Inject() (
                                       injector  : Injector,
                                     )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val bleUtil = injector.instanceOf[BleUtil]
  implicit private val ec = injector.instanceOf[ExecutionContext]


  /** Максимальное число результатов в ответе на запрос (макс. результатов на странице). */
  private def LIMIT_MAX           = 50

  /** Кол-во результатов на страницу по дефолту. */
  private def LIMIT_DFLT          = 20

  /** Макс.кол-во сдвигов в страницах. */
  private def OFFSET_MAX          = 20

  /** Максимальный абсолютный сдвиг в выдаче. */
  private def OFFSET_MAX_ABS      = OFFSET_MAX * LIMIT_MAX


  /**
    * Компиляция параметров поиска в MNodeSearch.
    * Код эвакуирован из models.AdSearch.qsb.bind().
    * @param args Данные по выборке карточек, пришедшие из qs.
    * @param innerHits Возвращать в ответе в inner_hits указанные поля с поддержкой doc_values.
    * @param bleSearchCtx Контекст поиска в BLE-маячках.
    */
  def qsArgs2nodeSearch(
                         args: MScQs,
                         innerHits: Option[MEsInnerHitsInfo] = None,
                         bleSearchCtx: MBleSearchCtx = MBleSearchCtx.empty,
                       ): MNodeSearch = {

    val _outEdges: Seq[Criteria] = {
      val must = IMust.MUST
      var eacc: List[Criteria] = Nil

      // Поиск карточек у указанного узла-ресивера.
      for {
        rcvrId <- args.search.rcvrId
        // 2018-03-19 Теги внутри узлов ищутся иначе, поэтому заданный ресивер отрабатывается в tagNodeId-ветви.
        if args.search.tagNodeId.isEmpty
      } {
        eacc ::= Criteria(
          nodeIds     = rcvrId.id :: Nil,
          predicates  = MPredicates.Receiver :: Nil,
          // Фильтрация по sls не нужна, они плавно уходят в прошлое.
          //anySl       = must,   // = Some(true)
          must        = must
        )
      }

      // Поиск карточек от указанного узла-продьюсера.
      for (prodId <- args.search.prodId) {
        eacc ::= Criteria(
          nodeIds     = prodId.id :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil,
          must        = must
        )
      }

      // Поддержка геопоиска в выдаче.
      args.search.tagNodeId.fold [Unit] {
        // Геотегов не указано. Но можно искать размещения карточек в указанной точке.
        for (geoLoc <- args.common.locEnv.geoLocOpt) {
          eacc ::= Criteria(
            predicates  = MPredicates.AdvGeoPlace :: Nil,
            must        = must,
            gsIntersect = Some(GsCriteria(
              levels = MNodeGeoLevels.geoPlacesSearchAt,
              shapes = GeoShapeJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil
            ))
          )
        }

      } { tagNodeId =>
        var preds = List.empty[MPredicate]

        val tagPredParent = MPredicates.TaggedBy

        if (args.search.rcvrId.nonEmpty)
          preds ::= tagPredParent.DirectTag

        if (args.common.locEnv.geoLocOpt.nonEmpty)
          preds ::= tagPredParent.AdvGeoTag

        // Указан тег. Ищем по тегу с учетом геолокации:
        eacc ::= Criteria(
          predicates = preds,
          nodeIds    = (tagNodeId :: args.search.rcvrId.toList)
            .map(_.id),
          nodeIdsMatchAll = true,
          must       = must,
          gsIntersect = for {
            geoLoc <- args.common.locEnv.geoLocOpt
          } yield {
            GsCriteria(
              levels = MNodeGeoLevels.geoTag :: Nil,
              shapes = GeoShapeJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil,
            )
          }
        )
      }

      // Вернуть получившийся и итоговый акк.
      eacc
    }

    // Общие константы выносим за скобки.

    val _nodeTypes  = MNodeTypes.Ad :: Nil

    val normalSearches = if (_outEdges.nonEmpty) {
      val normalSearch = new MNodeSearch {
        override val outEdges = MEsNestedSearch(
          clauses = _outEdges,
          innerHits = MEsInnerHitsInfo.buildInfoOpt( innerHits ),
        )
        override def nodeTypes = _nodeTypes
        override val randomSort = Some {
          MRandomSortData(
            generation = args.search.genOpt.getOrElse(1L),
            weight     = Some(0.0000001F)
          )
        }
        override val isEnabled = Some(true)
      }
      val subSearch = MSubSearch(
        search = normalSearch,
        must   = IMust.SHOULD
      )
      subSearch :: Nil
    } else {
      Nil
    }


    // Собрать итоговый запрос.
    val _limit = args.search.limit.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.search.offset.fold(0) {
      Math.min(OFFSET_MAX_ABS, _)
    }

    // Сборка поисков.
    val _subSearches = (
      normalSearches #::
      bleSearchCtx.subSearches #::
      LazyList.empty
    )
      .flatten

    // Собрать и вернуть результат.
    // Пока что всё работает синхронно.
    // Но для маячков скорее всего потребуется фоновая асинхронная работа по поиску id нод ble-маячков.
    new MNodeSearch {
      override def limit = _limit
      override def offset = _offset
      override def nodeTypes = _nodeTypes
      override def subSearches = _subSearches
    }
  }


  /** Генерация поисковых запросов по маячкам.
    *
    * Карточки в маячках ищутся отдельно от основного набора параметров, вне всяких продьюсеров-ресиверов-географии.
    * Результаты объединяются в общий выхлоп, но маячковые результаты -- поднимаются в начало этого списка.
    * Причём, чем ближе маячок -- тем выше результат.
    * @param innerHits Возвращать ли данные inner_hits?
    */
  def bleBeaconsSearch(bcnsQs: Seq[MUidBeacon], innerHits: Option[MEsInnerHitsInfo]): Future[MBleSearchCtx] = {
    if (bcnsQs.isEmpty) {
      Future.successful( MBleSearchCtx.empty )

    } else {
      import esModel.api._

      val _uidsQs = bcnsQs
        .iterator
        .map(_.node.nodeId)
        .toSet

      // Проверить id маячков: они должны быть существующими enabled узлами и иметь тип радио-маячков.
      val bcnUidsClearedFut = mNodes.dynSearchIds(
        new MNodeSearch {
          override val withIds    = _uidsQs.toSeq
          override val limit      = _uidsQs.size
          override val nodeTypes  = {
            val qsNodeTypes = bcnsQs.iterator.map(_.node.nodeType).toSet
            val allowedNodeTypes = MNodeTypes.lkNodesUserCanCreate.toSet
            (qsNodeTypes intersect allowedNodeTypes).toSeq
          }
          override val isEnabled  = Some(true)
        }
      )

      lazy val logPrefix = s"_bleBeacons2search(${bcnsQs.size}bcns)[${System.currentTimeMillis()}]:"
      LOGGER.trace(s"$logPrefix Beacons = ${bcnsQs.mkString(", ")}.\n Dirty bcn uids set: ${_uidsQs.mkString(", ")}")

      for {
        bcnsUidsClear <- bcnUidsClearedFut
      } yield {
        new MBleSearchCtx {
          override def uidsQs = _uidsQs

          override val uidsClear: Set[String] = {
            val _uidsClear = bcnsUidsClear.toSet
            LOGGER.trace(s"$logPrefix Cleared beacons set: ${_uidsClear.mkString(", ")}")
            _uidsClear
          }

          override lazy val bcnsQs2: Seq[MUidBeacon] = {
            // Учитывать только маячки до этого расстояния. Остальные не учитывать
            val maxDistance = BeaconUtil.DIST_CM_FAR

            // Фильтруем заявленные в qs id маячков по выверенному списку реально существующих маячков.
            // Ленивость bcnsQs2 не важна, т.к. коллекция сразу будет перемолота целиком в scoredByDistanceBeaconSearch()
            bcnsQs.filter { bcnQs =>
              val isExistBcn = uidsClear contains bcnQs.node.nodeId
              if (!isExistBcn)
                LOGGER.trace(s"$logPrefix Beacon uid'''${bcnQs.node.nodeId}''' is unknown. Dropped.")
              // fold(true), т.к. virtBeacon имеет отсутсвующее расстояние.
              isExistBcn && bcnQs.distanceCm.fold(true)(_ <= maxDistance)
            }
          }

          override lazy val subSearches: List[MSubSearch] = {
            // Не группируем тут по uid, т.к. это будет сделано внутри scoredByDistanceBeaconSearch()
            val _bcnsQs2 = bcnsQs2

            if (_bcnsQs2.isEmpty) {
              LOGGER.debug(s"$logPrefix Beacon uids was passed, but there are no known beacons.")
              Nil
            } else {
              val adsInBcnsSearchOpt = bleUtil.scoredByDistanceBeaconSearch(
                maxBoost    = 20000000F,
                // TODO Надо predicates = MPredicates.Receiver.AdvDirect :: Nil, но есть проблемы с LkNodes формой, которая лепит везде Self.
                predicates  = MPredicates.Receiver :: Nil,
                bcns        = _bcnsQs2,
                innerHits   = innerHits,
              )

              val sub = MSubSearch(
                search = adsInBcnsSearchOpt.get,
                must = IMust.SHOULD
              )

              sub :: Nil
            }
          }

        }
      }
    }
  }

}


/** Модель результата метода ScAdSearchUtil.bleBeaconsSearch().
  * Содержит в себе разные промежуточные значения обсчёта обстановки, которые могут быть нужны где-то выше по stacktrace.
  * Трейт, т.к. содержит в себе ленивые поля.
  */
protected trait MBleSearchCtx {
  /** id запрошенных в qs маячков. */
  def uidsQs      : Set[String] = Set.empty
  /** id найденных в БД маячков. */
  def uidsClear   : Set[String] = Set.empty
  /** Отчищенные от мусора qs-данные по мячкам. */
  def bcnsQs2     : Seq[MUidBeacon] = Nil
  /** Данные для поиска по маячкам в elasticsearch. */
  def subSearches : List[MSubSearch] = Nil
}
object MBleSearchCtx {
  def empty = new MBleSearchCtx {}
}


/** Интерфейс для поля c DI-инстансом [[ScAdSearchUtil]]. */
trait IScAdSearchUtilDi {
  def scAdSearchUtil: ScAdSearchUtil
}

