package util.showcase

import javax.inject.{Inject, Singleton}

import io.suggest.ble.{BeaconUtil, MUidBeacon}
import io.suggest.es.model.IMust
import io.suggest.es.search.{MRandomSortData, MSubSearch}
import io.suggest.geo.{MNodeGeoLevels, PointGs, PointGsJvm}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.msc.{MScAdsSearchQs, MScApiVsn}
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
  mNodes    : MNodes,
  bleUtil   : BleUtil,
  mCommonDi : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi.ec

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
  def qsArgs2nodeSearch(args: MScAdsSearchQs, apiVsnOpt: Option[MScApiVsn] = None): Future[MNodeSearch] = {

    val _outEdges: Seq[ICriteria] = {
      val must = IMust.MUST
      var eacc: List[Criteria] = Nil

      // Поиск карточек у указанного узла-ресивера.
      for (rcvrId <- args.rcvrIdOpt) {
        eacc ::= Criteria(
          nodeIds     = rcvrId.id :: Nil,
          predicates  = MPredicates.Receiver :: Nil,
          // Фильтрация по sls не нужна, они плавно уходят в прошлое.
          //anySl       = must,   // = Some(true)
          must        = must
        )
      }

      // Поиск карточек от указанного узла-продьюсера.
      for (prodId <- args.prodIdOpt) {
        eacc ::= Criteria(
          nodeIds     = prodId.id :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil,
          must        = must
        )
      }

      // Поддержка геопоиска в выдаче.
      args.tagNodeIdOpt.fold [Unit] {
        // Геотегов не указано. Но можно искать размещения карточек в указанной точке.
        for (geoLoc <- args.locEnv.geoLocOpt) {
          eacc ::= Criteria(
            predicates  = MPredicates.AdvGeoPlace :: Nil,
            must        = must,
            gsIntersect = Some(GsCriteria(
              levels = MNodeGeoLevels.geoPlace :: Nil,
              shapes = PointGsJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil
            ))
          )
        }

      } { tagNodeId =>
        // Указан тег. Ищем по тегу с учетом геолокации:
        eacc ::= Criteria(
          predicates = MPredicates.TaggedBy.Agt :: Nil,
          nodeIds    = tagNodeId :: Nil,
          must       = must,
          gsIntersect = for (geoLoc <- args.locEnv.geoLocOpt) yield {
            GsCriteria(
              levels = MNodeGeoLevels.geoTag :: Nil,
              shapes = PointGsJvm.toEsQueryMaker( PointGs(geoLoc.point) ) :: Nil
            )
          }
        )
      }

      // Фильтровать карточки под возможности текущего API: разруливать поддержку jd-формата по наличию jd-предиката:
      for (apiVsn <- apiVsnOpt) {
        eacc ::= Criteria(
          predicates  = MPredicates.JdContent :: Nil,
          must        = IMust.mustOrNot( apiVsn.useJdAds )
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
        generation = args.genOpt.getOrElse(1L),
        weight     = Some(0.0000001F)
      )
      val normalSearch = new MNodeSearchDfltImpl {
        override def outEdges         = _outEdges
        override def nodeTypes        = _nodeTypes
        override def randomSort       = Some(_mrs)
        override def isEnabled        = someTrue
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
    val bcnsSearchesFut = _bleBeacons2search( args.locEnv.bleBeacons )

    // Собрать итоговый запрос.
    val _limit = args.limitOpt.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.offsetOpt.fold(0) {
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
      new MNodeSearchDfltImpl {
        override def limit            = _limit
        override def offset           = _offset
        override def nodeTypes        = _nodeTypes
        override def subSearches      = _subSearches
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
        new MNodeSearchDfltImpl {
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

        val bcns2Iter = bcns
          .iterator
          .filter { bcn =>
            val isExistBcn = uids.contains( bcn.uid )
            if (!isExistBcn)
              LOGGER.trace(s"$logPrefix Beacon uid'''${bcn.uid}''' is unknown. Dropped.")
            isExistBcn && bcn.distanceCm <= maxDistance
          }
          // Не группируем тут по uid, т.к. это будет сделано внутри scoredByDistanceBeaconSearch()

        if (bcns2Iter.isEmpty) {
          LOGGER.debug(s"$logPrefix Beacon uids was passed, but there are no known beacons.")
          Nil
        } else {
          val adsInBcnsSearchOpt = bleUtil.scoredByDistanceBeaconSearch(
            maxBoost = 20000000F,
            predicates = MPredicates.Receiver.AdvDirect :: Nil,
            bcns = bcns2Iter
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
