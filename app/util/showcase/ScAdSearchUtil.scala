package util.showcase

import com.google.inject.{Inject, Singleton}
import io.suggest.common.radio.BeaconUtil
import io.suggest.model.es.IMust
import io.suggest.model.geo.PointGs
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.search.{MRandomSortData, MSubSearch}
import io.suggest.ym.model.NodeGeoLevels
import models.mgeo.MBleBeaconInfo
import models.msc.IScAdSearchQs
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
  bleUtil: BleUtil
) {

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
    */
  def qsArgs2nodeSearch(args: IScAdSearchQs): Future[MNodeSearch] = {

    val _outEdges: Seq[ICriteria] = {
      val must = IMust.MUST
      var eacc: List[Criteria] = Nil

      // Поиск карточек у указанного узла-ресивера.
      for (rcvrId <- args.rcvrIdOpt) {
        eacc ::= Criteria(
          nodeIds     = Seq( rcvrId.id ),
          predicates  = Seq( MPredicates.Receiver ),
          anySl       = must,   // = Some(true)
          must        = must
        )
      }

      // Поиск карточек от указанного узла-продьюсера.
      for (prodId <- args.prodIdOpt) {
        eacc ::= Criteria(
          nodeIds     = Seq( prodId.id ),
          predicates  = Seq( MPredicates.OwnedBy ),
          must        = must
        )
      }

      // Поддержка геопоиска в выдаче.
      args.tagNodeIdOpt.fold [Unit] {
        // Геотегов не указано. Но можно искать размещения карточек в указанной точке.
        for (geoLoc <- args.locEnv.geoLocOpt) {
          eacc ::= Criteria(
            predicates  = Seq( MPredicates.AdvGeoPlace ),
            must        = must,
            gsIntersect = Some(GsCriteria(
              levels = Seq( NodeGeoLevels.geoPlace ),
              shapes = Seq( PointGs(geoLoc.center) )
            ))
          )
        }

      } { tagNodeId =>
        // Указан тег. Ищем по тегу с учетом геолокации:
        eacc ::= Criteria(
          predicates = Seq( MPredicates.TaggedBy.Agt ),
          nodeIds    = Seq( tagNodeId ),
          must       = must,
          gsIntersect = for (geoLoc <- args.locEnv.geoLocOpt) yield {
            GsCriteria(
              levels = Seq( NodeGeoLevels.geoTag ),
              shapes = Seq( PointGs(geoLoc.center) )
            )
          }
        )
      }

      // Вернуть получившийся и итоговый акк.
      eacc
    }

    // Общие константы выносим за скобки.

    val _nodeTypes  = Seq( MNodeTypes.Ad )
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
    val bcnsSearches = _bleBeacons2search( args.locEnv.bleBeacons )

    // Собрать итоговый запрос.
    val _limit = args.limitOpt.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.offsetOpt.fold(0) {
      Math.min(OFFSET_MAX_ABS, _)
    }

    val _subSearches = Seq(
      normalSearches,
      bcnsSearches
    )
      .flatten

    // Собрать и вернуть результат.
    // Пока что всё работает синхронно.
    // Но для маячков скорее всего потребуется фоновая асинхронная работа по поиску id нод ble-маячков.
    val msearch = new MNodeSearchDfltImpl {
      override def limit            = _limit
      override def offset           = _offset
      override def nodeTypes        = _nodeTypes
      override def subSearches      = _subSearches
    }

    Future.successful(msearch)
  }


  /** Генерация поисковых запросов по маячкам. */
  def _bleBeacons2search(bcns: Iterable[MBleBeaconInfo]): Iterable[MSubSearch] = {
    if (bcns.isEmpty) {
      Nil

    } else {

      // Учитывать только маячки до этого расстояния.
      val maxDistance = BeaconUtil.DIST_CM_10M
      // Нужно проквантовать расстояния до маячков, группировать маячки по расстояниям, генерить поиски по группам маячков.
      val bcnGroups = bcns
        .iterator
        // Для вывода карточек наверх интересуют только маячки рядом.
        .filter(_.distanceCm <= maxDistance)
        .toSeq
        .groupBy( BeaconUtil.distanceToDistGroup )

      // Сконвертить группы маячков в search-реквесты узлов.
      bleUtil.byBeaconGroupsSearches(
        topScore  = 200000000F,
        predicate = MPredicates.AdvInRadioBeacon,
        bcnGroups = bcnGroups
      )
    }
  }


}


/** Интерфейс для поля c DI-инстансом [[ScAdSearchUtil]]. */
trait IScAdSearchUtilDi {
  def scAdSearchUtil: ScAdSearchUtil
}
