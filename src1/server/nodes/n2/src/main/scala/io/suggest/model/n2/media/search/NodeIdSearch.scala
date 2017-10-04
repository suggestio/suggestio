package io.suggest.model.n2.media.search

import io.suggest.es.model.{IMust, MWrapClause}
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.media.MMediaFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 14:14
  * Description: Поиск в MMedia по полю nodeId.
  */
trait NodeIdSearch extends DynSearchArgs {

  /** id узлов, объединяется через OR. */
  def nodeIds: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _nodeIds = nodeIds

    if (_nodeIds.isEmpty) {
      qbOpt0

    } else {
      // Есть перечисленные узлы. Собрать поиск по ним.
      val fn = MMediaFields.NODE_ID_FN

      val nodeIdsWraps = for (nodeId <- _nodeIds) yield {
        MWrapClause(
          must = IMust.SHOULD,
          queryBuilder = QueryBuilders.termQuery( fn, nodeId )
        )
      }

      val nodeIdsQb = IMust.maybeWrapToBool( nodeIdsWraps )

      qbOpt0
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must( qb0 )
            .must( nodeIdsQb )
        }
        .orElse {
          Some( nodeIdsQb )
        }
    }
  }

}


/** Дефолтовая реализация полей из [[NodeIdSearch]]. */
trait NodeIdSearchDflt extends NodeIdSearch {
  override def nodeIds: Seq[String] = Nil
}
