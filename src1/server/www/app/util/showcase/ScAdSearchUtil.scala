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


  /** Convert showcase query string args into MNodeSearch instance.
    * @param args Данные по выборке карточек, пришедшие из qs.
    * @param innerHits Возвращать в ответе в inner_hits указанные поля с поддержкой doc_values.
    * @param subSearches Other subsearch. Like result of bleSearchCtx.subSearches() for radio-beacons subsearches.
    */
  def qsArgs2nodeSearch(
                         args: MScQs,
                         innerHits: Option[MEsInnerHitsInfo] = None,
                         subSearches: List[MSubSearch] = Nil,
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
        val tagPredParent = MPredicates.TaggedBy
        var predsAcc = List.empty[MPredicate]

        if (args.search.rcvrId.nonEmpty)
          predsAcc ::= tagPredParent.DirectTag

        if (args.common.locEnv.geoLocOpt.nonEmpty)
          predsAcc ::= tagPredParent.AdvGeoTag

        // Указан тег. Ищем по тегу с учетом геолокации:
        eacc ::= Criteria(
          predicates = predsAcc,
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

    var _subSearchesAcc = subSearches

    if (_outEdges.nonEmpty) {
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
      _subSearchesAcc ::= subSearch
    }

    // Собрать итоговый запрос.
    val _limit = args.search.limit.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.search.offset.fold(0) {
      Math.min(OFFSET_MAX_ABS, _)
    }

    // Собрать и вернуть результат.
    // Пока что всё работает синхронно.
    // Но для маячков скорее всего потребуется фоновая асинхронная работа по поиску id нод ble-маячков.
    new MNodeSearch {
      override def limit = _limit
      override def offset = _offset
      override def nodeTypes = _nodeTypes
      override def subSearches = _subSearchesAcc
    }
  }


  /** Генерация поисковых запросов по маячкам.
    *
    * Карточки в маячках ищутся отдельно от основного набора параметров, вне всяких продьюсеров-ресиверов-географии.
    * Результаты объединяются в общий выхлоп, но маячковые результаты -- поднимаются в начало этого списка.
    * Причём, чем ближе маячок -- тем выше результат.
    */
  def radioBeaconsSearch(bcnsQs: Seq[MUidBeacon]): Future[MRadioBeaconsSearchCtx] = {
    if (bcnsQs.isEmpty) {
      Future.successful( MRadioBeaconsSearchCtx.empty )

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
        new MRadioBeaconsSearchCtx {
          override def uidsQs = _uidsQs

          override val uidsClear: Set[String] = {
            val _uidsClear = bcnsUidsClear.toSet
            LOGGER.trace(s"$logPrefix Cleared beacons set: ${_uidsClear.mkString(", ")}")
            _uidsClear
          }

          override lazy val qsBeacons2: Seq[MUidBeacon] = {
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

          override def subSearches(innerHits: Option[MEsInnerHitsInfo] = None,
                                   tagNodeId: Option[String] = None,
                                  ): List[MSubSearch] = {
            // Не группируем тут по uid, т.к. это будет сделано внутри scoredByDistanceBeaconSearch()
            val _qsBeacons2 = qsBeacons2

            if (_qsBeacons2.isEmpty) {
              LOGGER.debug(s"$logPrefix Beacon uids was passed, but there are no known beacons.")
              Nil
            } else {
              // Difference between tag search and main-screen search in predicate level and nodes-level:
              val pred = if (tagNodeId.isEmpty)
                // TODO Need MPredicates.Receiver.AdvDirect, but need to debug problems with LkNodes/LkAds forms, where Receiver.Self may be used.
                MPredicates.Receiver
              else
                MPredicates.TaggedBy.DirectTag

              val adsInBcnsSearchOpt = bleUtil.scoredByDistanceBeaconSearch(
                maxBoost    = 20000000F,
                predicates  = pred :: Nil,
                bcns        = _qsBeacons2,
                innerHits   = innerHits,
                tagNodeIdOpt = tagNodeId,
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
sealed trait MRadioBeaconsSearchCtx {
  /** id запрошенных в qs маячков. */
  def uidsQs      : Set[String] = Set.empty
  /** id найденных в БД маячков. */
  def uidsClear   : Set[String] = Set.empty
  lazy val uidsClearSeq = uidsClear.toList
  /** Отчищенные от мусора qs-данные по мячкам. */
  def qsBeacons2: Seq[MUidBeacon] = Nil

  /** SubSearches for beacons needs.
    *
    * @param innerHits If inner hits collection info needed, innerHits must be defined here.
    * @param tagNodeId If in-tag search, must contain tag node id.
    * @return MNodeSearch sub-searches list.
    */
  def subSearches(innerHits: Option[MEsInnerHitsInfo] = None,
                  tagNodeId: Option[String] = None,
                 ): List[MSubSearch] = Nil
}
object MRadioBeaconsSearchCtx {
  def empty = new MRadioBeaconsSearchCtx {}
}


/** Интерфейс для поля c DI-инстансом [[ScAdSearchUtil]]. */
trait IScAdSearchUtilDi {
  def scAdSearchUtil: ScAdSearchUtil
}

