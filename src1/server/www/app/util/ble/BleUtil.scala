package util.ble

import javax.inject.Singleton
import io.suggest.ble.MUidBeacon
import io.suggest.es.model.{MEsInnerHitsInfo, MEsNestedSearch}
import io.suggest.n2.edge.MPredicate
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import org.elasticsearch.common.lucene.search.function.{CombineFunction, FunctionScoreQuery}
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

import Ordering.Float.TotalOrdering

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
  def scoredByDistanceBeaconSearch(maxBoost: Float, predicates: Seq[MPredicate], bcns: Iterable[MUidBeacon],
                                   innerHits: Option[MEsInnerHitsInfo] = None): Option[MNodeSearch] = {
    Option.when( bcns.nonEmpty ) {
      // Строим карту маячков, где ключ -- Uid маячка
      val bcnsMap = (for {
        bcn <- bcns.iterator
        distanceCm <- bcn.distanceCm
      } yield {
        val dCm = Math.max(1, distanceCm)
        val weightFactor = 1.0F / dCm
        bcn.id -> weightFactor
      })
        .to( Iterable )
        .groupBy(_._1)
        .view
        .mapValues { values =>
          values
            .iterator
            .map(_._2)
            .min
        }
        .toMap

      // Итоговый поисковый запросец: поиск в эджах + кастомный скоринг поверх.
      new MNodeSearch {

        // Искать исходные ноды по наличию эджей на искомые маячки
        override val outEdges: MEsNestedSearch[Criteria] = {
          val cr = Criteria(
            predicates    = predicates,
            nodeIds       = bcnsMap.keys.toSeq
          )
          MEsNestedSearch(
            clauses   = cr :: Nil,
            innerHits = MEsInnerHitsInfo.buildInfoOpt( innerHits ),
          )
        }

        // Нетривиальный механизм function score для маячков реализован прямо здесь, потому что очень
        // специфический код и слишком много данных, проще и надежней прямо тут всё описать.
        override def toEsQuery: QueryBuilder = {
          val qb0 = super.toEsQuery

          // Собирать критерии скоринга: собрать фильтр по каждому маячку, выставив weight при срабатывании фильтра.
          val ffbs = bcnsMap.iterator
            .map { case (uid, weight) =>
              val bcnFilterDynSearch = new MNodeSearch {
                override val outEdges: MEsNestedSearch[Criteria] = {
                  val bcnEdgeCr = Criteria(
                    predicates = predicates,
                    nodeIds    = uid :: Nil,
                  )
                  MEsNestedSearch(
                    clauses = bcnEdgeCr :: Nil,
                  )
                }
              }
              val filter = bcnFilterDynSearch.toEsQuery
              val scorer = ScoreFunctionBuilders.weightFactorFunction(weight)
              new FilterFunctionBuilder(filter, scorer)
            }
            .toArray

          // Собирать итоговую function-score без function'ов внутри, только с весами и фильтрами внутрях.
          QueryBuilders.functionScoreQuery(qb0, ffbs)
            // При объединении скоров интересует только самый ближайший скор.
            .scoreMode( FunctionScoreQuery.ScoreMode.MAX )
            // При накладывании function-скора на найденную ноду, забывать об исходном скоринге, заменяя его кастомом.
            .boostMode( CombineFunction.REPLACE )
            .boost(maxBoost)
        }
      }
    }
  }

}


/** Интерфейс для поля с инжектируемым инстансом [[BleUtil]]. */
trait IBleUtilDi {
  def bleUtil: BleUtil
}
