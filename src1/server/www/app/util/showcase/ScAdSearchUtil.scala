package util.showcase

import javax.inject.{Inject, Singleton}
import io.suggest.ble.{BeaconUtil, MUidBeacon}
import io.suggest.es.model.{EsModel, IMust}
import io.suggest.es.search.{MRandomSortData, MSubSearch}
import io.suggest.geo.{MNodeGeoLevels, PointGs, PointGsJvm}
import io.suggest.model.n2.edge.{MPredicate, MPredicates}
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.sc.sc3.MScQs
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import util.ble.BleUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:18
  * Description: Утиль для поиска карточек в рамках выдачи.
  * Появлась в ходе распиливании исторически-запутанной модели models.AdSearch,
  * т.к. забинденные qs-данные нужно было приводить к MNodeSearch, что может потребовать исполнения
  * асинхронного кода (например, в случае маячков и ).
  */
@Singleton
class ScAdSearchUtil @Inject() (
                                 esModel   : EsModel,
                                 mNodes    : MNodes,
                                 bleUtil   : BleUtil,
                                 mCommonDi : ICommonDi
                               )
  extends MacroLogsImpl
{

  import mCommonDi.ec
  import esModel.api._

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
    * @param apiVsnOpt Версия API выдачи, если есть.
    *                  Например, нельзя выдавать jd-карточки в v2-выдачу, и наоборот.
    */
  def qsArgs2nodeSearch(args: MScQs): Future[MNodeSearch] = {

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
              shapes = PointGsJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil
            ))
          )
        }

      } { tagNodeId =>
        var preds = List.empty[MPredicate]

        val tagPredParent = MPredicates.TaggedBy

        if (args.search.rcvrId.nonEmpty)
          preds ::= tagPredParent.DirectTag

        if (args.common.locEnv.geoLocOpt.nonEmpty)
          preds ::= tagPredParent.Agt

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
              shapes = PointGsJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil
            )
          }
        )
      }

      // Вернуть получившийся и итоговый акк.
      eacc
    }

    // Общие константы выносим за скобки.

    val _nodeTypes  = MNodeTypes.Ad :: Nil
    val someTrue    = Some(true)

    val normalSearches = if (_outEdges.nonEmpty) {
      val _mrs = MRandomSortData(
        generation = args.search.genOpt.getOrElse(1L),
        weight     = Some(0.0000001F)
      )
      val normalSearch = new MNodeSearch {
        override def outEdges = _outEdges
        override def nodeTypes = _nodeTypes
        override def randomSort = Some(_mrs)
        override def isEnabled = someTrue
      }
      val subSearch = MSubSearch(
        search = normalSearch,
        must   = IMust.SHOULD
      )
      Seq(subSearch)
    } else {
      Nil
    }

    // Карточки в маячках ищутся отдельно от основного набора параметров, вне всяких продьюсеров-ресиверов-географии.
    // Результаты объединяются в общий выхлоп, но маячковые результаты -- поднимаются в начало этого списка.
    // Причём, чем ближе маячок -- тем выше результат.
    val bcnsSearchesFut = _bleBeacons2search( args.common.locEnv.bleBeacons )

    // Собрать итоговый запрос.
    val _limit = args.search.limit.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.search.offset.fold(0) {
      Math.min(OFFSET_MAX_ABS, _)
    }

    for {
      bcnsSearches <- bcnsSearchesFut
    } yield {
      val _subSearches = Seq(
        normalSearches,
        bcnsSearches
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
  }


  /** Генерация поисковых запросов по маячкам. */
  def _bleBeacons2search(bcns: Seq[MUidBeacon]): Future[Iterable[MSubSearch]] = {
    if (bcns.isEmpty) {
      Future.successful( Nil )

    } else {
      val uids = bcns.iterator
        .map(_.uid)
        .toSet
      // Проверить id маячков: они должны быть существующими enabled узлами и иметь тип радио-маячков.
      val bcnUidsClearedFut = mNodes.dynSearchIds(
        new MNodeSearch {
          override def withIds    = uids.toSeq
          override def limit      = uids.size
          override def nodeTypes  = MNodeTypes.BleBeacon :: Nil
          override def isEnabled  = Some(true)
        }
      )

      lazy val logPrefix = s"_bleBeacons2search(${bcns.size}bcns)[${System.currentTimeMillis()}]:"
      LOGGER.trace(s"$logPrefix Beacons = ${bcns.mkString(", ")}.\n Dirty bcn uids set: ${uids.mkString(", ")}")

      for {
        bcnsUidsClear <- bcnUidsClearedFut
      } yield {
        val uids = bcnsUidsClear.toSet
        LOGGER.trace(s"$logPrefix Cleared beacons set: ${uids.mkString(", ")}")

        // Учитывать только маячки до этого расстояния. Остальные не учитывать
        val maxDistance = BeaconUtil.DIST_CM_FAR

        val bcns2 = bcns
          .to(LazyList)
          .filter { bcn =>
            val isExistBcn = uids.contains( bcn.uid )
            if (!isExistBcn)
              LOGGER.trace(s"$logPrefix Beacon uid'''${bcn.uid}''' is unknown. Dropped.")
            isExistBcn && bcn.distanceCm <= maxDistance
          }
          // Не группируем тут по uid, т.к. это будет сделано внутри scoredByDistanceBeaconSearch()

        if (bcns2.isEmpty) {
          LOGGER.debug(s"$logPrefix Beacon uids was passed, but there are no known beacons.")
          Nil
        } else {
          val adsInBcnsSearchOpt = bleUtil.scoredByDistanceBeaconSearch(
            maxBoost = 20000000F,
            //TODO Надо predicates = MPredicates.Receiver.AdvDirect :: Nil, но есть проблемы с LkNodes формой, которая лепит везде Self.
            predicates = MPredicates.Receiver :: Nil,
            bcns = bcns2
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


/** Интерфейс для поля c DI-инстансом [[ScAdSearchUtil]]. */
trait IScAdSearchUtilDi {
  def scAdSearchUtil: ScAdSearchUtil
}
