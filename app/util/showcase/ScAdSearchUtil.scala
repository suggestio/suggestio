package util.showcase

import com.google.inject.Singleton
import io.suggest.model.geo.PointGs
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.ym.model.NodeGeoLevels
import models.msc.IScAdSearchQs

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
class ScAdSearchUtil /*@Inject() (
  mCommonDi : ICommonDi
)*/ {

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

    val _limit = args.limitOpt.fold(LIMIT_DFLT) {
      Math.min(LIMIT_MAX, _)
    }

    val _offset = args.offsetOpt.fold(0) {
      Math.min(OFFSET_MAX_ABS, _)
    }

    val _outEdges: Seq[ICriteria] = {
      val someTrue = Some(true)
      var eacc: List[Criteria] = Nil

      // Поиск карточек у указанного узла-ресивера.
      for (rcvrId <- args.rcvrIdOpt) {
        eacc ::= Criteria(
          nodeIds     = Seq( rcvrId.id ),
          predicates  = Seq( MPredicates.Receiver ),
          anySl       = someTrue,
          must        = someTrue
        )
      }

      // Поиск карточек от указанного узла-продьюсера.
      for (prodId <- args.prodIdOpt) {
        eacc ::= Criteria(
          nodeIds     = Seq( prodId.id ),
          predicates  = Seq( MPredicates.OwnedBy ),
          must        = someTrue
        )
      }

      // Можно искать размещения карточек в указанной точке.
      for (geoLoc <- args.locEnv.geoLocOpt) {
        eacc ::= Criteria(
          predicates  = Seq( MPredicates.AdvGeoPlace ),
          must        = someTrue,
          gsIntersect = Some(GsCriteria(
            levels = Seq( NodeGeoLevels.geoPlace ),
            shapes = Seq( PointGs(geoLoc.center) )
          ))
        )
      }

      // Вернуть получившийся и итоговый акк.
      eacc
    }

    val _nodeTypes  = Seq( MNodeTypes.Ad )
    val _genOpt     = args.genOpt

    // Собрать и вернуть результат.
    // Пока что всё работает синхронно.
    // Но для маячков скорее всего потребуется фоновая асинхронная работа по поиску id нод ble-маячков.
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges         = _outEdges
      override def limit            = _limit
      override def offset           = _offset
      override def nodeTypes        = _nodeTypes
      override def randomSortSeed   = _genOpt
    }

    Future.successful(msearch)
  }

}


/** Интерфейс для поля c DI-инстансом [[ScAdSearchUtil]]. */
trait IScAdSearchUtilDi {
  def scAdSearchUtil: ScAdSearchUtil
}
