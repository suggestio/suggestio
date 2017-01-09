package util.ble

import com.google.inject.Singleton
import io.suggest.model.n2.edge.MPredicate
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import models.mgeo.MBleBeaconInfo
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.16 16:01
  * Description: Утиль маячков в выдаче.
  */
@Singleton
class BleUtil {

  /**
    * Новый поиск нод маячков, завязанный на function-score и ровно один нижележащий
    * поисковый запрос для всех id маячков.
    */
  def scoredByDistanceBeaconSearch(maxBoost: Float, predicates: Seq[MPredicate], bcns: TraversableOnce[MBleBeaconInfo]): Option[MNodeSearch] = {
    if (bcns.isEmpty) {
      None

    } else {

      // Строим карту маячков, где ключ -- Uid маячка, а
      val bcnsMap = bcns
        .toIterator
        .map { bcn =>
          val dCm = Math.max(1, bcn.distanceCm)
          val weightFactor = 1.0F / dCm
          bcn.uid -> weightFactor
        }
        .toSeq
        .groupBy(_._1)
        .mapValues { vals =>
          vals.iterator
            .map(_._2)
            .min
        }


      // Итоговый поисковый запросец: поиск в эджах + кастомный скоринг поверх.
      val msearch = new MNodeSearchDfltImpl {

        // Искать исходные ноды по наличию эджей на искомые маячки
        override def outEdges = {
          val cr = Criteria(
            predicates    = predicates,
            nodeIds       = bcnsMap.keys.toSeq
          )
          Seq(cr)
        }

        // Нетривиальный механизм function score для маячков реализован прямо здесь, потому что очень
        // специфический код и слишком много данных, проще и надежней прямо тут всё описать.
        override def toEsQuery: QueryBuilder = {
          val qb0 = super.toEsQuery

          // Начинаем собирать function-score без function'ов внутри, только с весами и фильтрами внутрях.
          val qb1 = QueryBuilders.functionScoreQuery(qb0)
            // При объединении скоров интересует только самый ближайший скор.
            .scoreMode( "max" )
            // При накладывании function-скора на найденную ноду, забывать об исходном скоринге, заменяя его кастомом.
            .boostMode( "replace" )
            .boost(maxBoost)

          // Собирать критерии скоринга: собрать фильтр по каждому маячку, выставив weight при срабатывании фильтра.
          val qb2 = bcnsMap
            .foldLeft(qb1) { case (qbAcc, (uid, weight)) =>
              val bcnEdgeCr = Criteria(
                predicates = predicates,
                nodeIds    = Seq(uid)
              )
              val bcnFilterDynSearch = new MNodeSearchDfltImpl {
                override def outEdges = Seq(bcnEdgeCr)
              }
              val filter = bcnFilterDynSearch.toEsQuery
              val scorer = ScoreFunctionBuilders.weightFactorFunction(weight)
              qbAcc.add(filter, scorer)
            }

          qb2
        }
      }

      Some(msearch)
    }
  }

}


/** Интерфейс для поля с инжектируемым инстансом [[BleUtil]]. */
trait IBleUtilDi {
  def bleUtil: BleUtil
}
